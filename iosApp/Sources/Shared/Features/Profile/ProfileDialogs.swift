import Shared
import SwiftUI

extension View {
    /// The profile's dialogs come from the shared state: closing any of them tells the ViewModel.
    func profileDialogs(model: ProfileModel) -> some View {
        modifier(ProfileDialogsModifier(model: model))
    }
}

private struct ProfileDialogsModifier: ViewModifier {
    let model: ProfileModel

    func body(content: Content) -> some View {
        let state = model.state
        content
            .alert("Who sees your profile", isPresented: isPresented(.visibilityInfo)) {
                Button("Got it") {}
            } message: {
                Text("Your name, your photo and your payment keys only show to people in your groups. Anyone who shares no group with you sees nothing.")
            }
            .alert("Sign out?", isPresented: isPresented(.signOut)) {
                Button("Cancel", role: .cancel) {}
                Button("Sign out", role: .destructive) {
                    model.send(ProfileUiEventOnSignOutConfirmed.shared)
                }
            } message: {
                let provider = state.accountProvider?.displayName ?? ""
                if state.hasSignOutFailed {
                    Text("Couldn't sign out. Try again.")
                } else {
                    Text("Everything is synced with your \(provider) account. When you sign out, we erase the copy on this \(DeviceName.current). Your groups come back when you sign in again.")
                }
            }
            .alert(pendingTitle(count: Int(state.pendingChangeCount)), isPresented: isPresented(.signOutPending)) {
                Button("Cancel", role: .cancel) {}
                Button("Sign out once synced") {
                    model.send(ProfileUiEventOnSignOutWhenSyncedClicked.shared)
                }
                // A sync that never succeeds (offline for good, a rejected push) can't keep the person signed in
                Button("Sign out now and lose the changes", role: .destructive) {
                    model.send(ProfileUiEventOnSignOutNowClicked.shared)
                }
            } message: {
                if state.hasSignOutFailed {
                    Text("Couldn't sign out. Try again.")
                } else if state.isOnline {
                    Text("Your data stays on this \(DeviceName.current) until the sync finishes. Once everything is uploaded, we sign out and erase the local copy.")
                } else {
                    Text("You're offline. Your data stays on this \(DeviceName.current) until the sync finishes. Once everything is uploaded, we sign out and erase the local copy.")
                }
            }
            .sheet(isPresented: isPresented(.eraseProfile)) {
                EraseProfileSheet(model: model)
                    .foldSheetPlacement()
            }
    }

    private func pendingTitle(count: Int) -> Text {
        Text("\(count) changes haven't uploaded yet")
    }

    /// An alert closes itself on any of its buttons, so the close waits for the button's own event to
    /// reach the ViewModel first, and the sign out alerts, which can't show progress, stay hidden while
    /// it runs; a failure brings the alert back with the error.
    private func isPresented(_ dialog: ProfileDialog) -> Binding<Bool> {
        Binding(
            get: {
                let state = model.state
                let isSignOutAlert = dialog == .signOut || dialog == .signOutPending
                let isBusy = isSignOutAlert && state.isSigningOut
                return state.dialog == dialog && !isBusy
            },
            set: { isPresented in
                guard !isPresented else { return }
                DispatchQueue.main.async {
                    if model.state.dialog == dialog {
                        model.send(ProfileUiEventOnDialogDismissed.shared)
                    }
                }
            },
        )
    }
}

/// Without Google or Apple the profile only comes back with the recovery code, so "Sign out" ("Erase and sign out")
/// waits for the switch saying it is kept, or that losing it is understood when there is none yet.
private struct EraseProfileSheet: View {
    let model: ProfileModel

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let state = model.state
        NavigationStack {
            Form {
                Section {
                    if state.hasPendingChanges {
                        Text("\(Int(state.pendingChangeCount)) changes haven't uploaded yet and will be lost.")
                            .foregroundStyle(Palette.negative)
                    }
                    if let code = state.formattedRecoveryCode {
                        Text("Your account has no Google or Apple sign in. When you sign out, we erase the copy on this \(DeviceName.current). To come back, you need the recovery code.")
                        HStack {
                            Text(verbatim: code)
                                .font(.system(.title3, design: .monospaced).weight(.semibold))
                                .tracking(2)
                                .textSelection(.enabled)
                            Spacer()
                            Button(model.didCopy ? "Copied" : "Copy" as LocalizedStringKey) {
                                model.send(ProfileUiEventOnCopyCodeClicked.shared)
                            }
                            .buttonStyle(.borderless)
                        }
                    } else {
                        Text("You use it without an account and haven't synced yet, so there is no recovery code. When you leave, your profile is gone for good.")
                    }
                    Toggle(
                        state.hasRecoveryCode
                            ? "I kept the code somewhere safe" as LocalizedStringKey
                            : "I understand it can't be recovered",
                        isOn: Binding(
                            get: { model.state.hasConfirmedCodeSaved },
                            set: { model.send(ProfileUiEventOnCodeSavedChanged(isChecked: $0)) },
                        ),
                    )
                }
                Section {
                    Button("Connect Apple or Google instead") {
                        model.send(ProfileUiEventOnConnectInsteadClicked.shared)
                    }
                    .disabled(!state.canConnect)
                    Button(role: .destructive) {
                        model.send(ProfileUiEventOnEraseConfirmed.shared)
                    } label: {
                        if state.isSigningOut {
                            ProgressView()
                        } else {
                            Text(state.erasesProfileOnSignOut ? "Erase and sign out" : "Sign out" as LocalizedStringKey)
                        }
                    }
                    .disabled(!state.canErase)
                } footer: {
                    if state.hasSignOutFailed {
                        Text("Couldn't sign out. Try again.").foregroundStyle(Palette.negative)
                    }
                }
            }
            .formStyle(.grouped)
            // An anonymous account signs out like any other and comes back with its code; only without one is it erased
            .navigationTitle(
                state.erasesProfileOnSignOut
                    ? "Erase the profile from this \(DeviceName.current)?"
                    : "Sign out?" as LocalizedStringKey,
            )
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) { dismiss() }
                        .disabled(state.isSigningOut)
                }
            }
        }
        #if os(iOS)
        .presentationDetents([.medium, .large])
        #else
        .frame(width: 460, height: 420)
        #endif
        .interactiveDismissDisabled(state.isSigningOut)
    }
}
