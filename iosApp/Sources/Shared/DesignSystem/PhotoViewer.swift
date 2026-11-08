import SwiftUI

/// A photo big, as Bible Planner's viewer, for the profile's and the group's: the photo with the shortcuts that change
/// it at once under it. On iPhone and iPad it covers the screen over the blurred page, closed by ✕ or a tap outside;
/// on the Mac it is a sheet, closed by Close or Esc. `content` gets the photo's side: 260 only where both sides of the
/// screen have 600, so an iPhone lying down keeps the compact photo and its shortcuts fit.
struct PhotoViewer<Content: View>: View {
    let dismiss: () -> Void
    @ViewBuilder let content: (CGFloat) -> Content
    @State private var isWide = false

    var body: some View {
        #if os(iOS)
        GeometryReader { proxy in
            // Scrolls the photo whole where the screen is short (an iPhone lying down with the error line)
            ScrollView {
                content(isWide ? 260 : 200)
                    // A tap between the shortcuts stays on the photo; only one around it closes the viewer
                    .contentShape(.rect)
                    .onTapGesture {}
                    .padding(.vertical, 16)
                    .frame(maxWidth: .infinity, minHeight: proxy.size.height)
                    .contentShape(.rect)
                    .onTapGesture(perform: dismiss)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .background(Color.black.opacity(0.3).ignoresSafeArea())
        .overlay(alignment: .topTrailing) {
            Button("Close", systemImage: "xmark", action: dismiss)
                .labelStyle(.iconOnly)
                .font(.system(size: 18, weight: .semibold))
                .frame(width: 44, height: 44)
                .glassEffect(.regular.interactive(), in: .circle)
                .padding(14)
        }
        .environment(\.colorScheme, .dark)
        .onGeometryChange(for: Bool.self, of: { min($0.size.width, $0.size.height) >= 600 }) { isWide = $0 }
        .presentationBackground(.ultraThinMaterial)
        #else
        content(200)
            .padding(32)
            .frame(minWidth: 440)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: dismiss)
                }
            }
        #endif
    }
}

/// The viewer's shortcuts under the photo: what the caller puts first (the account's photo), "Camera" where there is
/// one, "Gallery" and, with a photo, "Remove".
struct PhotoViewerShortcuts<Leading: View>: View {
    let hasPhoto: Bool
    @Binding var takesPhoto: Bool
    @Binding var showsPhotoPicker: Bool
    let remove: () -> Void
    @ViewBuilder let leading: Leading

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            leading
            #if os(iOS)
            if CameraPicker.isAvailable {
                PhotoShortcut(title: Text("Camera"), systemImage: "camera", accessibilityLabel: "Take photo") {
                    takesPhoto = true
                }
            }
            #endif
            // The system picker needs no photo library permission and returns one image, or nothing
            PhotoShortcut(title: Text("Gallery"), systemImage: "photo.on.rectangle", accessibilityLabel: "Choose photo") {
                showsPhotoPicker = true
            }
            if hasPhoto {
                PhotoShortcut(
                    title: Text("Remove"),
                    systemImage: "trash",
                    accessibilityLabel: "Remove photo",
                    isDestructive: true,
                    action: remove,
                )
            }
        }
    }
}

/// The photo's error line under the viewer's photo; each caller draws its own spinner over the photo.
struct PhotoViewerStatus: View {
    let showsError: Bool

    var body: some View {
        if showsError {
            Text("Couldn't use this photo. Try another one.")
                .font(.footnote)
                .foregroundStyle(Palette.negative)
                .multilineTextAlignment(.center)
        }
    }
}

extension View {
    /// The photo's gallery picker and, on iOS, the camera, which hand a picked photo (or nothing) to `picked`.
    func photoViewerPickers(
        showsPhotoPicker: Binding<Bool>,
        takesPhoto: Binding<Bool>,
        picked: @escaping (Data?) -> Void
    ) -> some View {
        photoLibraryPicker(isPresented: showsPhotoPicker, onPicked: picked)
            #if os(iOS)
            .cameraCapture(isRequested: takesPhoto, onPicked: picked)
            #endif
    }
}

/// A round glass button with its title under it, as Bible Planner's viewer draws its shortcuts.
struct PhotoShortcut: View {
    let title: Text
    let systemImage: String
    let accessibilityLabel: LocalizedStringKey
    var isDestructive = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 22, weight: .medium))
                    .frame(width: 58, height: 58)
                    .glassEffect(
                        isDestructive ? .regular.tint(Palette.negative.opacity(0.35)).interactive() : .regular.interactive(),
                        in: .circle,
                    )
                title
                    .font(.caption.weight(.medium))
            }
            .foregroundStyle(isDestructive ? Palette.negative : .primary)
            .frame(width: 76)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}
