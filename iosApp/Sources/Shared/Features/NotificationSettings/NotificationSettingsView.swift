import Observation
import Shared
import SwiftUI
import UserNotifications

#if os(iOS)
import UIKit
#else
import AppKit
#endif

@Observable
final class NotificationSettingsModel {
    private(set) var state: NotificationSettingsUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createNotificationSettingsViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! NotificationSettingsUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            self?.handle(action)
        })
    }

    func send(_ event: any NotificationSettingsUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    /// Read on appear and on every return to the app, since the person may change it in Settings.
    func checkPermission() {
        Task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            let permission: NotificationPermission = switch settings.authorizationStatus {
            case .notDetermined: .notDetermined
            case .denied: .denied
            default: .granted
            }
            send(NotificationSettingsUiEventOnPermissionChecked(permission: permission))
        }
    }

    private func handle(_ action: any NotificationSettingsUiAction) {
        switch action {
        case is NotificationSettingsUiActionRequestPermission:
            Task {
                _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
                checkPermission()
            }
        case is NotificationSettingsUiActionOpenSystemSettings:
            openSystemSettings()
        default:
            break
        }
    }

    private func openSystemSettings() {
        #if os(iOS)
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else { return }
        UIApplication.shared.open(url)
        #else
        let bundleId = Bundle.main.bundleIdentifier ?? ""
        let address = "x-apple.systempreferences:com.apple.Notifications-Settings.extension?id=\(bundleId)"
        guard let url = URL(string: address) else { return }
        NSWorkspace.shared.open(url)
        #endif
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Notifications" (4.8), pushed from the profile on iPhone and iPad. The Mac shows the same sections
/// inside its settings, as the canvas draws them.
struct NotificationSettingsView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: NotificationSettingsModel?

    var body: some View {
        Group {
            if let model {
                Form {
                    NotificationSettingsSections(model: model)
                }
                .formStyle(.grouped)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .navigationTitle("Notifications")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .onAppear {
            if model == nil { model = NotificationSettingsModel() }
        }
    }
}

/// The denied notice (4.8b) and the four toggles, for a Form of the screen or of the Mac's settings.
struct NotificationSettingsSections: View {
    let model: NotificationSettingsModel

    var body: some View {
        let state = model.state
        Group {
            if state.showsPermissionDenied {
                Section {
                    PermissionDeniedNotice {
                        model.send(NotificationSettingsUiEventOnOpenSettingsClicked.shared)
                    }
                }
            }
            Section {
                ForEach(state.types, id: \.self) { type in
                    Toggle(isOn: Binding(
                        get: { state.isEnabled(type: type) },
                        set: { model.send(NotificationSettingsUiEventOnToggled(type: type, isEnabled: $0)) },
                    )) {
                        VStack(alignment: .leading, spacing: 1) {
                            Text(type.title)
                            Text(type.detail)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .disabled(!state.areTogglesEnabled)
            } header: {
                Group {
                    #if os(macOS)
                    Text("Notifications")
                    #else
                    Text("Notify me when")
                    #endif
                }
                // On the one header always there: a Form hands a section's modifiers to each of its rows
                .modifier(PermissionChecks(model: model))
            } footer: {
                Text("Notices arrive when there's internet. To mute a group, use the menu inside it.")
            }
        }
    }
}

/// Reads the permission when the screen or section appears and on every return to the app: the Mac's
/// scene stays active while the person is in System Settings, so it listens to the app becoming active.
private struct PermissionChecks: ViewModifier {
    let model: NotificationSettingsModel

    func body(content: Content) -> some View {
        content
            .onAppear { model.checkPermission() }
            .onReceive(NotificationCenter.default.publisher(for: Self.didBecomeActive)) { _ in
                model.checkPermission()
            }
    }

    #if os(macOS)
    private static let didBecomeActive = NSApplication.didBecomeActiveNotification
    #else
    private static let didBecomeActive = UIApplication.didBecomeActiveNotification
    #endif
}

private struct PermissionDeniedNotice: View {
    let openSettings: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "bell.slash")
                    .foregroundStyle(Palette.negative)
                    .frame(width: 30, height: 30)
                    .background(Palette.negative.opacity(0.12), in: .rect(cornerRadius: 8))
                #if os(macOS)
                Text("Blitz Split notifications are turned off in System Settings.")
                #else
                Text("Blitz Split notifications are turned off in the \(DeviceName.current) Settings.")
                #endif
            }
            .accessibilityElement(children: .combine)
            Button(action: openSettings) {
                #if os(macOS)
                Text("Open System Settings")
                #else
                Text("Open Settings")
                    .frame(maxWidth: .infinity)
                #endif
            }
            .buttonStyle(.glassProminent)
            #if os(iOS)
            .controlSize(.large)
            #endif
        }
        .padding(.vertical, 4)
    }
}

private extension NotificationType {
    var title: LocalizedStringKey {
        switch self {
        case .theNewExpense: "New expense in my groups"
        case .paymentMarked: "Someone marked a payment to me"
        case .settleReminders: "Settle-up reminders"
        default: "Invites and new members"
        }
    }

    var detail: LocalizedStringKey {
        switch self {
        case .theNewExpense: "When someone adds an expense with you"
        case .paymentMarked: "When someone says they've paid you"
        case .settleReminders: "Once a week, if there's an open balance"
        default: "When someone joins one of your groups"
        }
    }
}
