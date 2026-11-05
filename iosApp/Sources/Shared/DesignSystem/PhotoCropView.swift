import ImageIO
import Shared
import SwiftUI

/// A profile's photo shows in a circle, a group's in the rounded square of `GroupAvatar`.
enum PhotoCropFrame {
    case circle
    case roundedSquare
}

/// "Adjust photo" (Bible Planner's editor): a picked or taken photo framed before it becomes the profile's or the
/// group's. Dark, as the Photos editor is: full screen on iOS and a 560×720 sheet on the Mac. Where the photo sits is
/// the ViewModel's (`PhotoCrop`); here only the drag and the pinch are read, in fractions of the frame's side.
struct PhotoCropView: View {
    let editor: PhotoCropEditor
    let frame: PhotoCropFrame
    let send: (PhotoCropEvent) -> Void

    /// The editor's photo, decoded once per pick: it is upright already and at most 1600 px.
    @State private var image: Image?
    // Why: gesture state, so a drag or a pinch the system cancels (no onEnded) starts the next one afresh.
    @GestureState private var lastDrag: CGSize = .zero
    @GestureState private var lastMagnification: CGFloat = 1
    /// A pinch is on: the drag, which follows one of its fingers, waits until it ends.
    @GestureState private var isMagnifying = false

    private static let background = Color(red: 11 / 255, green: 11 / 255, blue: 15 / 255)
    private static let frameRatio: CGFloat = 0.9
    private static let maxFrameSide: CGFloat = 360
    private static let panelWidth: CGFloat = 360
    /// GroupAvatar's corners, a share of its side.
    private static let roundedSquareCornerRatio: CGFloat = 0.32

    var body: some View {
        GeometryReader { proxy in
            let isWide = proxy.size.width > proxy.size.height
            Group {
                if isWide {
                    // An iPhone Duo open lying down puts the photo and the controls each on its side of the fold
                    TwoColumnsLayout(secondWidth: Self.panelWidth, spacing: 0, maxWidth: .infinity, alignment: .center) {
                        cropArea
                    } second: {
                        VStack(spacing: 0) {
                            header
                            controls
                        }
                    }
                } else {
                    // Open upright, as a notebook, the photo above the fold and the controls below it
                    FoldHalves(scrollsHalves: false) {
                        cropArea
                    } bottom: {
                        VStack(spacing: 0) {
                            header
                            controls
                        }
                        .frame(maxHeight: .infinity)
                    } single: {
                        VStack(spacing: 0) {
                            header
                            cropArea
                            controls
                        }
                    }
                }
            }
        }
        .background(Self.background.ignoresSafeArea())
        .foregroundStyle(.white)
        .environment(\.colorScheme, .dark)
        .task(id: ObjectIdentifier(editor.photo)) {
            let jpeg = BlitzSplitApple.shared.toData(bytes: editor.photo.jpeg) as Data
            // Why: a 1600 px JPEG decoded on the main thread hitches the cover sliding in.
            image = await Task.detached(priority: .userInitiated) { Self.decode(jpeg) }.value
        }
        #if os(macOS)
        .frame(width: 560, height: 720)
        #endif
    }

    private var header: some View {
        ZStack {
            Text("Adjust photo")
                .font(.headline)
            HStack {
                Button("Close", systemImage: "xmark") { send(PhotoCropEventOnDismissed.shared) }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.glass)
                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 56)
    }

    private var cropArea: some View {
        GeometryReader { proxy in
            let side = min(proxy.size.width * Self.frameRatio, proxy.size.height * Self.frameRatio, Self.maxFrameSide)
            let crop = editor.crop
            ZStack {
                if let image {
                    image
                        .resizable()
                        .frame(
                            width: CGFloat(crop.photoWidthInFrame) * side,
                            height: CGFloat(crop.photoHeightInFrame) * side,
                        )
                        // Why: mirrored, turned, zoomed, then moved, in this order, as the ViewModel's model.
                        .scaleEffect(x: CGFloat(crop.zoom) * (crop.isMirrored ? -1 : 1), y: CGFloat(crop.zoom))
                        .rotationEffect(.degrees(Double(crop.rotationDegrees)))
                        .offset(x: CGFloat(crop.offsetX) * side, y: CGFloat(crop.offsetY) * side)
                }
                frameMask(side: side)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
            .clipped()
            .contentShape(Rectangle())
            .gesture(dragGesture(side: side).simultaneously(with: magnifyGesture(side: side, size: proxy.size)))
        }
    }

    /// The scrim with the frame cut out of it, and the frame's border.
    private func frameMask(side: CGFloat) -> some View {
        let frameShape = frame == .circle
            ? AnyShape(Circle())
            : AnyShape(RoundedRectangle(cornerRadius: side * Self.roundedSquareCornerRatio))
        return ZStack {
            Rectangle()
                .fill(Self.background.opacity(0.72))
                .overlay {
                    frameShape
                        .frame(width: side, height: side)
                        .blendMode(.destinationOut)
                }
                .compositingGroup()
            frameShape
                .stroke(.white.opacity(0.9), lineWidth: 2)
                .frame(width: side, height: side)
        }
        .allowsHitTesting(false)
    }

    private func dragGesture(side: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .updating($lastDrag) { value, lastDrag, _ in
                let dx = value.translation.width - lastDrag.width
                let dy = value.translation.height - lastDrag.height
                lastDrag = value.translation
                guard side > 0, !isMagnifying else { return }
                send(PhotoCropEventOnDragged(dx: Float(dx / side), dy: Float(dy / side)))
            }
    }

    /// Zooms around the point between the fingers, from the frame's center (the area's) in fractions of its side.
    private func magnifyGesture(side: CGFloat, size: CGSize) -> some Gesture {
        MagnifyGesture()
            .updating($isMagnifying) { _, isMagnifying, _ in isMagnifying = true }
            .updating($lastMagnification) { value, lastMagnification, _ in
                let change = value.magnification / lastMagnification
                lastMagnification = value.magnification
                guard side > 0 else { return }
                send(
                    PhotoCropEventOnPinched(
                        zoomChange: Float(change),
                        focusX: Float((value.startLocation.x - size.width / 2) / side),
                        focusY: Float((value.startLocation.y - size.height / 2) / side),
                    ),
                )
            }
    }

    private var controls: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "photo")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.7))
                Slider(
                    value: Binding(
                        get: { Double(editor.crop.zoom) },
                        set: { send(PhotoCropEventOnZoomChanged(zoom: Float($0))) },
                    ),
                    in: Double(editor.crop.minZoom)...Double(editor.crop.maxZoom),
                )
                .tint(.white)
                .accessibilityLabel("Zoom")
                Image(systemName: "photo")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.7))
            }
            Text("Drag and zoom to frame")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.6))
            HStack(spacing: 8) {
                orientationButton("Horizontal", systemImage: "arrow.left.and.right.righttriangle.left.righttriangle.right") {
                    send(PhotoCropEventOnMirrorHorizontallyClicked.shared)
                }
                orientationButton("Vertical", systemImage: "arrow.up.and.down.righttriangle.up.righttriangle.down") {
                    send(PhotoCropEventOnMirrorVerticallyClicked.shared)
                }
                orientationButton("Rotate", systemImage: "rotate.right") {
                    send(PhotoCropEventOnTurnClicked.shared)
                }
            }
            HStack(spacing: 12) {
                Button {
                    send(PhotoCropEventOnDismissed.shared)
                } label: {
                    Text("Cancel").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glass)
                // Why: a Mac sheet closes on Esc only through its cancel action.
                .keyboardShortcut(.cancelAction)
                Button {
                    send(PhotoCropEventOnConfirmClicked.shared)
                } label: {
                    Text("Done").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .prominentLabel()
                .keyboardShortcut(.defaultAction)
            }
            .controlSize(.large)
            .padding(.top, 4)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }

    private func orientationButton(_ title: LocalizedStringKey, systemImage: String, action: @escaping () -> Void) -> some View {
        Button(title, systemImage: systemImage, action: action)
            .font(.footnote)
            .buttonStyle(.glass)
    }

    /// Decoded whole here, since an image made from data only decodes when first drawn. The photo is upright already.
    private nonisolated static func decode(_ data: Data) -> Image? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let decoded = CGImageSourceCreateImageAtIndex(
                  source,
                  0,
                  [kCGImageSourceShouldCacheImmediately: true] as CFDictionary,
              )
        else { return nil }
        return Image(decorative: decoded, scale: 1)
    }
}

extension View {
    /// Opens "Adjust photo" while a picked photo is being framed: full screen on iOS, a sheet on the Mac. Closing it any
    /// way (Cancel, ✕, a swipe, Esc) is the ViewModel's `OnDismissed`, which leaves the photo shown as it was.
    func photoCrop(_ editor: PhotoCropEditor?, frame: PhotoCropFrame, send: @escaping (PhotoCropEvent) -> Void) -> some View {
        let isPresented = Binding(
            get: { editor != nil },
            set: { if !$0 { send(PhotoCropEventOnDismissed.shared) } },
        )
        #if os(macOS)
        return sheet(isPresented: isPresented) {
            if let editor { PhotoCropView(editor: editor, frame: frame, send: send) }
        }
        #else
        return fullScreenCover(isPresented: isPresented) {
            if let editor { PhotoCropView(editor: editor, frame: frame, send: send) }
        }
        #endif
    }
}
