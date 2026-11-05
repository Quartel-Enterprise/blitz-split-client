import Observation
import Shared
import SwiftUI

@Observable
final class InAppUpdateModel {
    private(set) var state: InAppUpdateUiState

    @ObservationIgnored private let holder: ViewModelHolder<InAppUpdateViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteInAppUpdate) {
        holder = BlitzSplitApple.shared.createInAppUpdateViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! InAppUpdateUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { action in
            guard let open = action as? InAppUpdateUiActionOpenStore, let url = URL(string: open.url) else { return }
            #if os(iOS)
            UIApplication.shared.open(url)
            #else
            NSWorkspace.shared.open(url)
            #endif
        })
    }

    func send(_ event: any InAppUpdateUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Update available", as Bible Planner's: the App Store's newer version, "Open in the App Store" and "Not now". A
/// sheet as tall as its content on an iPhone, a form sheet with an iPad's room and a sheet on the Mac (on Ajustes when
/// "Check for updates" opened it there). Only its buttons close it (Esc is "Not now"), so each answer is counted once,
/// and a sheet the window draws again never counts as one.
struct InAppUpdateView: View {
    let route: RouteInAppUpdate

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: InAppUpdateModel?
    @State private var contentHeight: CGFloat = 320

    var body: some View {
        Group {
            if let model {
                InAppUpdateContent(model: model)
                    .onGeometryChange(for: CGFloat.self, of: \.size.height) { height in
                        contentHeight = height
                    }
            } else {
                Color.clear
            }
        }
        #if os(iOS)
        .presentationDetents([.height(contentHeight)])
        .formSheetSizing()
        #else
        .frame(width: 420)
        #endif
        .interactiveDismissDisabled()
        .onAppear {
            if model == nil { model = InAppUpdateModel(route: route) }
        }
    }
}

private struct InAppUpdateContent: View {
    let model: InAppUpdateModel

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "arrow.down.app.fill")
                .font(.system(size: 30))
                .foregroundStyle(Palette.accent)
                .frame(width: 56, height: 56)
                .background(Palette.accent.opacity(0.14), in: .rect(cornerRadius: 16))
                .accessibilityHidden(true)
            VStack(spacing: 4) {
                Text("Update available")
                    .font(.title2.bold())
                    .accessibilityAddTraits(.isHeader)
                if let versionName = model.state.versionName {
                    Text("Version \(versionName)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Text("You'll be taken to the App Store to install the new version.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            VStack(spacing: 8) {
                Button {
                    model.send(InAppUpdateUiEventOnUpdateClicked.shared)
                } label: {
                    Label("Open in the App Store", systemImage: "arrow.down.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
                .controlSize(.large)
                .keyboardShortcut(.defaultAction)
                Button("Not now") {
                    model.send(InAppUpdateUiEventOnDismissed.shared)
                }
                .buttonStyle(.borderless)
                .controlSize(.large)
                .keyboardShortcut(.cancelAction)
            }
            .padding(.top, 8)
        }
        .padding(.horizontal, 24)
        .padding(.top, 28)
        .padding(.bottom, 16)
        .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
        .frame(maxWidth: .infinity)
    }
}
