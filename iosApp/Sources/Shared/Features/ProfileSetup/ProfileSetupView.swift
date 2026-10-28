import Observation
import PhotosUI
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
                                // Why: the pinned "Continue" would otherwise hide the field above the keyboard
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

/// "Continue" stays on screen below the scrolling content, above the keyboard when it is open.
private struct ProfileFooter: View {
    let model: ProfileSetupModel

    var body: some View {
        let state = model.state
        VStack(spacing: 10) {
            Text("Saved only on this \(DeviceName.current) for now.")
                .font(.footnote)
                .foregroundStyle(.secondary)
            PrimaryButton(
                title: "Continue",
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
            Text("What do your friends call you?")
                .font(isLarge ? .largeTitle.bold() : .title.bold())
            Text("Only people in your groups see your name.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 4)
    }
}

private struct AvatarSection: View {
    let model: ProfileSetupModel
    let avatarSize: CGFloat

    @State private var pickedPhoto: PhotosPickerItem?

    var body: some View {
        let state = model.state
        VStack(spacing: 14) {
            ZStack(alignment: .bottomTrailing) {
                AvatarPhoto(
                    initial: state.avatarInitial,
                    color: state.avatarColor,
                    photoUrl: state.photoUrl,
                    size: avatarSize,
                )
                .overlay {
                    if state.isPreparingPhoto { ProgressView() }
                }
                // Why: the system picker needs no photo library permission and returns one image, or nothing.
                PhotosPicker(selection: $pickedPhoto, matching: .images) {
                    AddPhotoLabel()
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Add photo")
                .offset(x: 6, y: 6)
            }
            .onChange(of: pickedPhoto) { _, item in
                guard let item else { return }
                pickedPhoto = nil
                Task {
                    guard let data = try? await item.loadTransferable(type: Data.self) else {
                        model.send(ProfileSetupUiEventOnPhotoUnreadable.shared)
                        return
                    }
                    model.send(ProfileSetupUiEventOnPhotoPicked(source: BlitzSplitApple.shared.toByteArray(data: data)))
                }
            }
            if state.showsPhotoError {
                Text("Couldn't use this photo. Try another one.")
                    .font(.footnote)
                    .foregroundStyle(Palette.negative)
                    .multilineTextAlignment(.center)
            }
            SectionCaption(text: "Avatar color")
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
            .accessibilityLabel("Avatar color")
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
            SectionCaption(text: "Your name")
            GroupedCard {
                HStack(spacing: 8) {
                    TextField(
                        "John Doe",
                        text: Binding(
                            get: { model.state.name },
                            set: { model.send(ProfileSetupUiEventOnNameChanged(name: $0)) },
                        ),
                    )
                    .textContentType(.name)
                    .focused($isFocused)
                    .submitLabel(.done)
                    .onSubmit { model.send(ProfileSetupUiEventOnContinueClicked.shared) }
                    .accessibilityLabel("Your name")
                    if state.nameError != nil {
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
            Text(nameHint(state))
                .font(.footnote)
                .foregroundStyle(state.nameError != nil ? Palette.negative : .secondary)
                .padding(.horizontal, 20)
        }
    }

    private func nameHint(_ state: ProfileSetupUiState) -> LocalizedStringKey {
        switch state.nameError {
        case .tooShort: "Type at least \(Int(state.nameMinLength)) letters."
        case .tooLong: "Use up to \(Int(state.nameMaxLength)) characters."
        default: "You can change it later in Profile."
        }
    }
}

/// Its own view because the picker's label closure is Sendable, outside the main actor the palette lives on.
private struct AddPhotoLabel: View {
    var body: some View {
        Image(systemName: "camera.fill")
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(Palette.onAccent)
            .frame(width: 44, height: 44)
            .background(Palette.accent, in: .rect(cornerRadius: 14))
            .overlay {
                RoundedRectangle(cornerRadius: 14).strokeBorder(Palette.groupedBackground, lineWidth: 3)
            }
    }
}
