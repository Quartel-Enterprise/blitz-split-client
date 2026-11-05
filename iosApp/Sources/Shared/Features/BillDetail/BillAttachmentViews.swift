import Shared
import SwiftUI
import UniformTypeIdentifiers

/// The words of Bill's Attachments and its viewer (7.3): the shared code hands over who added what and when.
extension BillCopy {
    /// "2 of 5 photos"; the receipt isn't one of them. Nil while the attachments load.
    static func photoCount(_ state: BillDetailUiState) -> String? {
        guard let count = state.photoCount?.intValue else { return nil }
        return String(localized: "\(count) of \(Int(state.photoLimit)) photos")
    }

    /// "Photo 2", or "Store receipt".
    static func attachmentName(_ attachment: BillAttachmentTile) -> String {
        guard let number = attachment.photoNumber?.intValue else { return String(localized: "Store receipt") }
        return String(localized: "Photo \(number)", comment: "One of a bill's photos, by the order it was added")
    }

    /// "Photo 1 of 2" over the viewer, or "Store receipt".
    static func viewerTitle(_ attachment: BillAttachmentTile, photoCount: Int32) -> String {
        guard let number = attachment.photoNumber?.intValue else { return String(localized: "Store receipt") }
        return String(localized: "Photo \(number) of \(Int(photoCount))", comment: "The bill's photo viewer's title")
    }

    /// "You", whoever added it, or "Someone" once they left the group.
    static func adder(_ attachment: BillAttachmentTile) -> String {
        if attachment.isAddedByMe { return String(localized: "You") }
        return attachment.adderName ?? String(localized: "Someone", comment: "Who did something in Activity, unnamed")
    }

    /// "Ana · Yesterday" under a tile.
    static func attachmentCaption(_ attachment: BillAttachmentTile) -> String {
        "\(adder(attachment)) · \(ExpenseFormat.day(attachment.addedAt, relativeDay: attachment.addedDay))"
    }

    /// The viewer's "Ana attached it yesterday at 10:40 PM", or "Bruno scanned the receipt today at 9:10 PM".
    static func attached(_ attachment: BillAttachmentTile) -> String {
        let who = adder(attachment)
        let time = ExpenseFormat.time(attachment.addedAt)
        let date = ExpenseFormat.shortDate(attachment.addedAt)
        if attachment.isReceipt {
            return switch attachment.addedDay {
            case .today: String(localized: "\(who) scanned the receipt today at \(time)")
            case .yesterday: String(localized: "\(who) scanned the receipt yesterday at \(time)")
            default: String(localized: "\(who) scanned the receipt on \(date) at \(time)")
            }
        }
        return switch attachment.addedDay {
        case .today: String(localized: "\(who) attached it today at \(time)")
        case .yesterday: String(localized: "\(who) attached it yesterday at \(time)")
        default: String(localized: "\(who) attached it on \(date) at \(time)")
        }
    }

    /// What a screen reader says for a tile: "Photo 2, Ana · Yesterday, waiting to go up".
    static func attachmentLabel(_ attachment: BillAttachmentTile) -> String {
        let label = "\(attachmentName(attachment)), \(attachmentCaption(attachment))"
        guard attachment.isPending else { return label }
        return "\(label), \(String(localized: "waiting to go up", comment: "A bill's photo not on the server yet"))"
    }

    /// "bora-rachar-foto-2.jpg" in Portuguese and "blitz-split-photo-2.jpg" in any other language, as the data export
    /// names its file.
    static func attachmentFileName(_ attachment: BillAttachmentTile) -> String {
        let isPortuguese = Bundle.main.preferredLocalizations.first?.hasPrefix("pt") == true
        let name = switch attachment.photoNumber?.intValue {
        case let number?: isPortuguese ? "foto-\(number)" : "photo-\(number)"
        case nil: isPortuguese ? "cupom" : "receipt"
        }
        return (isPortuguese ? "bora-rachar-" : "blitz-split-") + name + ".jpg"
    }
}

/// What went wrong with a photo, said in the glass toast of Bill (and of the viewer over it).
enum BillAttachmentToasts {
    static func messages(_ model: BillDetailModel?) -> [GlassToastMessage] {
        [
            GlassToastMessage(text: "Couldn't open this image. Choose another one.", trigger: model?.unreadablePhotoCount ?? 0),
            GlassToastMessage(text: "This photo is too large. Pick another one.", trigger: model?.tooLargePhotoCount ?? 0),
            GlassToastMessage(
                text: "This bill already has \(Int(model?.state.photoLimit ?? 5)) photos.",
                trigger: model?.noRoomPhotoCount ?? 0,
            ),
            GlassToastMessage(text: "Couldn't attach the photo. Try again.", trigger: model?.failedPhotoCount ?? 0),
            removeError(model),
        ]
    }

    static func removeError(_ model: BillDetailModel?) -> GlassToastMessage {
        GlassToastMessage(text: "Couldn't remove the photo. Try again.", trigger: model?.removePhotoErrorCount ?? 0)
    }
}

/// The sizes of Attachments: the iPhone's and iPad's tiles with their name and who added them under, or the Mac's
/// smaller thumbnails, named in their tooltip.
struct BillAttachmentMetrics {
    let tile: CGSize
    let cornerRadius: CGFloat

    #if os(macOS)
    static let standard = BillAttachmentMetrics(tile: CGSize(width: 72, height: 54), cornerRadius: 8)
    static let narrow = BillAttachmentMetrics(tile: CGSize(width: 64, height: 48), cornerRadius: 8)
    #else
    static let standard = BillAttachmentMetrics(tile: CGSize(width: 100, height: 76), cornerRadius: 14)
    #endif
}

/// "Attachments" with "2 of 5 photos" on its right.
struct BillAttachmentsHeader: View {
    let state: BillDetailUiState

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("Attachments", comment: "The bill's photos section")
            Spacer(minLength: 8)
            if let count = BillCopy.photoCount(state) {
                Text(verbatim: count)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            } else {
                ShimmerLine(width: 80, height: 10)
                    .shimmering()
            }
        }
    }
}

/// The receipt first, then the photos in the order they were added, then "Add" while the bill has room: a strip that
/// scrolls sideways in one column, wrapping onto a second line where the bill has a column of its own.
struct BillAttachmentStrip: View {
    let model: BillDetailModel
    let wraps: Bool
    let metrics: BillAttachmentMetrics

    var body: some View {
        if wraps {
            TermFlowLayout(spacing: 10) { tiles }
        } else {
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: 10) { tiles }
            }
            .scrollIndicators(.hidden)
            .scrollClipDisabled()
        }
    }

    @ViewBuilder
    private var tiles: some View {
        let state = model.state
        if state.attachments.isLoading {
            // The attachments not read yet: tiles shimmer in their place, and "Add" waits for the count
            ForEach(0..<2, id: \.self) { _ in
                ShimmerBlock(width: metrics.tile.width, height: metrics.tile.height, cornerRadius: metrics.cornerRadius)
                    .shimmering()
            }
        }
        ForEach(state.loadedAttachments ?? [], id: \.id) { attachment in
            BillAttachmentTileView(attachment: attachment, metrics: metrics) {
                model.send(BillDetailUiEventOnAttachmentClicked(id: attachment.id))
            }
        }
        if state.showsAddPhoto {
            BillAddPhotoTile(model: model, metrics: metrics)
        }
    }
}

/// One attachment, cropped to fill its tile: "Photo 2" with "Ana · Yesterday" under it (in the tooltip on the Mac),
/// the receipt's badge, or the amber dot of a photo still waiting to go up.
private struct BillAttachmentTileView: View {
    let attachment: BillAttachmentTile
    let metrics: BillAttachmentMetrics
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            #if os(macOS)
            thumbnail
            #else
            VStack(alignment: .leading, spacing: 1) {
                thumbnail
                    .padding(.bottom, 4)
                Text(verbatim: BillCopy.attachmentName(attachment))
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(verbatim: BillCopy.attachmentCaption(attachment))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(width: metrics.tile.width, alignment: .leading)
            #endif
        }
        .buttonStyle(.plain)
        #if os(macOS)
        .help(Text(verbatim: "\(BillCopy.attachmentName(attachment)) · \(BillCopy.adder(attachment))"))
        #endif
        .accessibilityLabel(Text(verbatim: BillCopy.attachmentLabel(attachment)))
    }

    private var thumbnail: some View {
        AttachmentImage(attachment: attachment, contentMode: .fill) {
            Image(systemName: attachment.isReceipt ? "receipt" : "photo")
                .font(.title3)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(width: metrics.tile.width, height: metrics.tile.height)
        .background(Palette.card)
        .clipShape(.rect(cornerRadius: metrics.cornerRadius))
        .overlay(alignment: .bottomLeading) {
            if attachment.isPending {
                AttachmentBadge(isPending: true)
            } else if attachment.isReceipt {
                AttachmentBadge(isPending: false)
            }
        }
        .contentShape(.rect(cornerRadius: metrics.cornerRadius))
    }
}

/// "Waiting" ("Pending" on the Mac) with the amber dot of what is queued, or the receipt's "Receipt".
private struct AttachmentBadge: View {
    let isPending: Bool

    var body: some View {
        HStack(spacing: 4) {
            if isPending {
                Circle()
                    .fill(Palette.pendingOnDark)
                    .frame(width: 6, height: 6)
                pendingWord
            } else {
                Text("Receipt", comment: "The badge on the receipt a bill was read from, among its photos")
            }
        }
        #if os(macOS)
        .font(.system(size: 10, weight: .semibold))
        .padding(.horizontal, 5)
        .frame(height: 15)
        .foregroundStyle(isPending ? .white : Palette.onAccent)
        .background(isPending ? Color.black.opacity(0.8) : Palette.accent, in: .rect(cornerRadius: 4))
        .padding(3)
        #else
        .font(.system(size: 11, weight: .semibold))
        .padding(.horizontal, 7)
        .frame(height: 20)
        .foregroundStyle(isPending ? .white : Palette.onAccent)
        .background(isPending ? Color.black.opacity(0.8) : Palette.accent, in: .capsule)
        .padding(6)
        #endif
        .accessibilityHidden(true)
    }

    private var pendingWord: Text {
        #if os(macOS)
        Text("Pending", comment: "A bill's photo not on the server yet, on its Mac thumbnail")
        #else
        Text("Waiting", comment: "A bill's photo not on the server yet, on its tile")
        #endif
    }
}

/// "Add" with a dashed border: take a photo (where there is a camera) or choose one from Photos, and on the Mac a
/// file too. A spinner while the photo picked is being made ready.
private struct BillAddPhotoTile: View {
    let model: BillDetailModel
    let metrics: BillAttachmentMetrics

    @State private var showsPhotoPicker = false
    #if os(iOS)
    @State private var takesPhoto = false
    #else
    @State private var importsFile = false
    #endif

    var body: some View {
        Menu {
            #if os(iOS)
            if CameraPicker.isAvailable {
                Button("Take photo", systemImage: "camera") { takesPhoto = true }
            }
            #else
            Button("Choose File…", systemImage: "folder") { importsFile = true }
            #endif
            Button("Choose from Photos", systemImage: "photo.on.rectangle") { showsPhotoPicker = true }
        } label: {
            label
        }
        .menuStyle(.button)
        .buttonStyle(.plain)
        .menuIndicator(.hidden)
        .disabled(!model.state.canAddPhoto)
        #if os(macOS)
        .help(Text("Add photo"))
        #endif
        .accessibilityLabel(Text("Add photo"))
        .photoLibraryPicker(isPresented: $showsPhotoPicker) { data in model.photoPicked(data) }
        #if os(iOS)
        .cameraCapture(isRequested: $takesPhoto, device: .rear) { data in model.photoPicked(data) }
        #else
        .fileImporter(isPresented: $importsFile, allowedContentTypes: [.image]) { result in
            // A cancelled panel picked nothing, so it says nothing either
            if case let .failure(error) = result, (error as? CocoaError)?.code == .userCancelled { return }
            model.photoPicked(Self.read(result))
        }
        #endif
    }

    @ViewBuilder
    private var label: some View {
        #if os(macOS)
        frame
        #else
        VStack(alignment: .leading, spacing: 1) {
            frame
                .padding(.bottom, 4)
            Text("Add", comment: "The tile that adds a photo to a bill")
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text("Camera or photos", comment: "Under the tile that adds a photo to a bill")
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(width: metrics.tile.width, alignment: .leading)
        #endif
    }

    private var frame: some View {
        ZStack {
            if model.state.isAddingPhoto {
                ProgressView()
                    .controlSize(.small)
            } else {
                Image(systemName: "plus")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Palette.accent)
            }
        }
        .frame(width: metrics.tile.width, height: metrics.tile.height)
        .background(Palette.card, in: .rect(cornerRadius: metrics.cornerRadius))
        .overlay {
            RoundedRectangle(cornerRadius: metrics.cornerRadius)
                .strokeBorder(.secondary, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
        }
        .contentShape(.rect(cornerRadius: metrics.cornerRadius))
    }

    #if os(macOS)
    /// The file the open panel picked, read within the access the sandbox grants it; nil when it can't be read.
    private static func read(_ result: Result<URL, any Error>) -> Data? {
        guard case let .success(url) = result else { return nil }
        let isScoped = url.startAccessingSecurityScopedResource()
        defer { if isScoped { url.stopAccessingSecurityScopedResource() } }
        return try? Data(contentsOf: url)
    }
    #endif
}
