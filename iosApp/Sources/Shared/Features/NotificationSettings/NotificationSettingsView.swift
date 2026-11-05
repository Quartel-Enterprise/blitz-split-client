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

/// "Notifications" (4.8), pushed from the profile and from Activity on iPhone and iPad. The Mac shows the same
/// sections inside its settings, which Activity opens on them.
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

/// The denied notice (4.8b), which only stops the pushes, and a row per kind of Activity (and the weekly reminder),
/// each with a Push and an In app toggle under the column names, for a Form of the screen or of the Mac's settings.
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
                    NotificationRow(type: type, state: state) { channel, isOn in
                        model.send(NotificationSettingsUiEventOnToggled(type: type, channel: channel, isEnabled: isOn))
                    }
                }
            } header: {
                HStack(alignment: .bottom, spacing: NotificationRow.cellSpacing) {
                    Group {
                        #if os(macOS)
                        Text("Notifications")
                        #else
                        Text("Notify me about")
                        #endif
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(state.channels, id: \.self) { channel in
                        Text(channel.title)
                            .font(.caption2.weight(.semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                            .frame(width: NotificationRow.cellWidth)
                    }
                }
                // On the one header always there: a Form hands a section's modifiers to each of its rows
                .modifier(PermissionChecks(model: model))
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Push sends a notification to this device. In app marks it new on the bell and in Activity; turned off, it still shows in Activity, already read.")
                    Text("Notices arrive when there's internet. To mute a group, use the menu inside it.")
                }
            }
        }
    }
}

/// One kind with its two toggles: an icon button each, filled when on and crossed out when off, so the state never
/// rests on the color alone. Shimmers until the choices (a restored account's come with its first full sync) and the
/// system's permission are known, so no toggle shows a default that a tap would save over the server's.
private struct NotificationRow: View {
    static let cellWidth: CGFloat = 52
    static let cellSpacing: CGFloat = 4

    let type: NotificationType
    let state: NotificationSettingsUiState
    let toggle: (NotificationChannel, Bool) -> Void

    var body: some View {
        HStack(spacing: Self.cellSpacing) {
            VStack(alignment: .leading, spacing: 1) {
                Text(type.title)
                Text(type.detail)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: Self.cellSpacing) {
                ForEach(state.channels, id: \.self) { channel in
                    cell(channel)
                        .frame(width: Self.cellWidth)
                }
            }
            // One shimmer for the row's cells, as the other lists load
            .modifier(LoadingShimmer(isLoading: state.showsTogglesLoading))
        }
    }

    @ViewBuilder
    private func cell(_ channel: NotificationChannel) -> some View {
        if !state.hasToggle(type: type, channel: channel) {
            // The weekly reminder never shows in Activity, so it has no In app toggle
            Color.clear.frame(height: 1)
        } else if state.showsTogglesLoading {
            ShimmerBlock(width: 36, height: 36, cornerRadius: 18)
        } else {
            let isOn = state.isEnabled(type: type, channel: channel)
            Toggle(isOn: Binding(get: { isOn }, set: { toggle(channel, $0) })) {
                Label {
                    Text(channel.title)
                } icon: {
                    Image(systemName: channel.symbol(isOn: isOn))
                        .contentTransition(.symbolEffect(.replace))
                }
                .labelStyle(.iconOnly)
            }
            .toggleStyle(.button)
            // A Form row with two buttons of the automatic style fires both on a tap anywhere in it; a bordered one
            // takes only its own taps, and draws the circle
            .buttonStyle(.bordered)
            .buttonBorderShape(.circle)
            .disabled(!state.areTogglesEnabled(channel: channel))
            .accessibilityLabel(Text("\(Text(type.title)), \(Text(channel.title))"))
        }
    }
}

/// Shimmers its content only while it loads, so a loaded row draws no animation.
private struct LoadingShimmer: ViewModifier {
    let isLoading: Bool

    func body(content: Content) -> some View {
        if isLoading {
            content.shimmering()
        } else {
            content
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
                Text("Blitz Split notifications are turned off in System Settings. The bell keeps working.")
                #else
                Text("Blitz Split notifications are turned off in the \(DeviceName.current) Settings. The bell keeps working.")
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
            .prominentLabel()
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
        case .bills: "Bills"
        case .payments: "Payments and settle-ups"
        case .comments: "Comments"
        case .peopleAndGroups: "People and groups"
        case .paymentRequests: "Payment requests"
        default: "Open balance reminder"
        }
    }

    var detail: LocalizedStringKey {
        switch self {
        case .bills: "New, edited or deleted bills you're in"
        case .payments: "Payments to or from you, and simplified debts"
        case .comments: "On bills you're in and threads you wrote in"
        case .peopleAndGroups: "Who joins or leaves your groups, and what changes in them"
        case .paymentRequests: "When someone asks you to pay"
        default: "Once a week, if there's an open balance"
        }
    }
}

private extension NotificationChannel {
    var title: LocalizedStringKey {
        switch self {
        case .push: "Push"
        default: "In app"
        }
    }

    func symbol(isOn: Bool) -> String {
        switch self {
        case .push:
            #if os(macOS)
            isOn ? "laptopcomputer" : "laptopcomputer.slash"
            #else
            isOn ? "iphone" : "iphone.slash"
            #endif
        default:
            isOn ? "bell.fill" : "bell.slash"
        }
    }
}
