import Observation
import Shared
import SwiftUI

@Observable
final class ProfileSetupModel {
    private(set) var state: ProfileSetupUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createProfileSetupViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ProfileSetupUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    func send(_ event: any ProfileSetupUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

struct ProfileSetupView: View {
    @State private var model = ProfileSetupModel()
    @State private var layout = ScreenLayout()

    var body: some View {
        VStack(spacing: 0) {
            content
            ProfileFooter(model: model)
        }
        .background(Palette.groupedBackground)
        .measuringScreenLayout($layout)
    }

    @ViewBuilder
    private var content: some View {
        Group {
            if layout.usesTwoColumns {
                HStack(alignment: .center, spacing: 48) {
                    ScrollView {
                        AvatarSection(model: model, avatarSize: layout.isLarge ? 144 : 104)
                            .padding(.vertical, 24)
                            .frame(maxWidth: .infinity)
                            .background(Palette.card, in: .rect(cornerRadius: 32))
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .frame(maxWidth: 340)
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            ProfileTitle(isLarge: layout.isLarge)
                            NameSection(model: model)
                        }
                    }
                    .scrollBounceBehavior(.basedOnSize)
                }
                .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
                .padding(.horizontal, 32)
            } else {
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            ProfileTitle(isLarge: false)
                            AvatarSection(model: model, avatarSize: 112)
                            NameSection(model: model) {
                                // Why: the pinned "Continuar" would otherwise hide the field above the keyboard
                                withAnimation { proxy.scrollTo(NameSection.scrollId, anchor: .center) }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                }
                .frame(maxWidth: ScreenLayout.singleColumnMaxWidth)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// "Continuar" stays on screen below the scrolling content, above the keyboard when it is open.
private struct ProfileFooter: View {
    let model: ProfileSetupModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            Text("Fica salvo só neste \(DeviceName.current) por enquanto.")
                .font(.footnote)
                .foregroundStyle(.secondary)
            PrimaryButton(
                title: "Continuar",
                isEnabled: state.canContinue,
                isLoading: state.isSaving,
            ) {
                model.send(ProfileSetupUiEventOnContinueClicked.shared)
            }
            .frame(maxWidth: 400)
        }
        .padding(.horizontal, 24)
        .padding(.top, 8)
        .padding(.bottom, 8)
    }
}

private struct ProfileTitle: View {
    let isLarge: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Como seus amigos te chamam?")
                .font(isLarge ? .largeTitle.bold() : .title.bold())
            Text("Seu nome aparece só para quem estiver nos seus grupos.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 4)
    }
}

private struct AvatarSection: View {
    let model: ProfileSetupModel
    let avatarSize: CGFloat

    var body: some View {
        let state = model.state
        VStack(spacing: 14) {
            Text(state.avatarInitial)
                .font(.system(size: avatarSize * 0.42, weight: .bold))
                .foregroundStyle(state.avatarColor.content)
                .frame(width: avatarSize, height: avatarSize)
                .background(state.avatarColor.container, in: .circle)
                .accessibilityHidden(true)
            SectionCaption(text: "Cor do avatar")
            GroupedCard {
                HStack {
                    ForEach(state.avatarColors, id: \.self) { color in
                        ColorSwatch(color: color, isSelected: color == state.avatarColor) {
                            model.send(ProfileSetupUiEventOnAvatarColorSelected(color: color))
                        }
                        if color != state.avatarColors.last { Spacer(minLength: 8) }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel("Cor do avatar")
        }
        .animation(.snappy, value: state.avatarColor)
    }
}

private struct ColorSwatch: View {
    let color: AvatarColor
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Circle()
                .fill(color.container)
                .frame(width: 40, height: 40)
                .overlay {
                    if isSelected {
                        Image(systemName: "checkmark")
                            .font(.headline)
                            .foregroundStyle(color.content)
                    }
                }
                .overlay {
                    Circle().strokeBorder(Palette.accent, lineWidth: isSelected ? 3 : 0)
                }
                .padding(2)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(color.label)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

private struct NameSection: View {
    static let scrollId = "nameField"

    let model: ProfileSetupModel
    var onFocus: () -> Void = {}

    @FocusState private var isFocused: Bool

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 7) {
            SectionCaption(text: "Seu nome")
            GroupedCard {
                HStack(spacing: 8) {
                    TextField(
                        "Ex.: Pierre",
                        text: Binding(
                            get: { model.state.name },
                            set: { model.send(ProfileSetupUiEventOnNameChanged(name: $0)) },
                        ),
                    )
                    .textContentType(.name)
                    .focused($isFocused)
                    .submitLabel(.done)
                    .onSubmit { model.send(ProfileSetupUiEventOnContinueClicked.shared) }
                    .accessibilityLabel("Seu nome")
                    if state.showsNameTooShort {
                        Image(systemName: "exclamationmark.circle.fill")
                            .foregroundStyle(Palette.negative)
                            .accessibilityHidden(true)
                    }
                }
                .font(.body)
                .padding(.horizontal, 16)
                .frame(minHeight: 52)
            }
            .id(Self.scrollId)
            .onChange(of: isFocused) { _, focused in
                guard focused else { return }
                Task {
                    try? await Task.sleep(for: .milliseconds(350))
                    onFocus()
                }
            }
            Text(state.showsNameTooShort ? "Digite pelo menos 2 letras." : "Você pode mudar depois em Perfil.")
                .font(.footnote)
                .foregroundStyle(state.showsNameTooShort ? Palette.negative : .secondary)
                .padding(.horizontal, 20)
        }
    }
}
