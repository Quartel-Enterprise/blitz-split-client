import CoreTransferable
import Shared
import SwiftUI
import UniformTypeIdentifiers

#if os(macOS)
import AppKit

typealias AttachmentPlatformImage = NSImage
#else
import UIKit

typealias AttachmentPlatformImage = UIImage
#endif

/// A photo added on this device and not up yet, held as its JPEG and its decoded image.
///
/// Why: the tile hands the JPEG as a KotlinByteArray, the same instance on every state, and turning it into `Data`
/// copies it, so each attachment is copied and decoded once, not on every redraw of the strip or the
/// viewer. Keyed by the attachment's id, since the photo it holds never changes; a hit wins even once the tile has no
/// bytes left (the photo went up), so the strip doesn't load it again from the signed URL.
private enum DecodedAttachments {
    static let jpegs: NSCache<NSString, NSData> = {
        let cache = NSCache<NSString, NSData>()
        cache.totalCostLimit = 64 * 1024 * 1024
        return cache
    }()

    static let images = NSCache<NSString, AttachmentPlatformImage>()

    /// The JPEG of `id`, copied out of `bytes` on the first call only; nil while neither the cache nor the tile has it.
    static func jpeg(id: String, bytes: KotlinByteArray?) -> Data? {
        if let cached = jpegs.object(forKey: id as NSString) { return cached as Data }
        guard let bytes else { return nil }
        let data = copy(bytes)
        jpegs.setObject(data as NSData, forKey: id as NSString, cost: data.count)
        return data
    }

    static func image(id: String, bytes: KotlinByteArray?) -> AttachmentPlatformImage? {
        if let cached = images.object(forKey: id as NSString) { return cached }
        guard let data = jpeg(id: id, bytes: bytes), let image = AttachmentPlatformImage(data: data) else { return nil }
        images.setObject(image, forKey: id as NSString)
        return image
    }

    /// One copy on the Kotlin side (`NSData(bytes:length:)`), not a call per byte.
    private static func copy(_ bytes: KotlinByteArray) -> Data {
        BlitzSplitApple.shared.toData(bytes: bytes) as Data
    }
}

/// A bill's photo, cropped to fill a tile or fit in the viewer: the JPEG added on this device while it has one, else
/// the signed URL; `placeholder` while it loads, when it fails, and for a photo never loaded on this device while
/// offline (no bytes and a nil URL).
struct AttachmentImage<Placeholder: View>: View {
    let attachment: BillAttachmentTile
    let contentMode: ContentMode
    @ViewBuilder let placeholder: () -> Placeholder

    var body: some View {
        if let image = DecodedAttachments.image(id: attachment.id, bytes: attachment.localImage) {
            drawn(image: platformImage(image))
        } else if let url = attachment.imageUrl, let remote = URL(string: url) {
            AsyncImage(url: remote) { phase in
                if let image = phase.image {
                    drawn(image: image)
                } else {
                    placeholder()
                }
            }
        } else {
            placeholder()
        }
    }

    private func drawn(image: Image) -> some View {
        image
            .resizable()
            .aspectRatio(contentMode: contentMode)
    }

    private func platformImage(_ image: AttachmentPlatformImage) -> Image {
        #if os(macOS)
        Image(nsImage: image)
        #else
        Image(uiImage: image)
        #endif
    }
}

/// "Download": the attachment as a JPEG named "blitz-split-photo-2.jpg" ("bora-rachar-foto-2.jpg"), for the share
/// sheet's Save Image on iOS and the save panel on the Mac. The shared code always stores a JPEG.
nonisolated struct AttachmentFile: Transferable {
    enum Source {
        /// The JPEG added on this device, exported as it is.
        case jpeg(Data)
        /// The signed URL, downloaded once the person saves.
        case remote(URL)
    }

    let source: Source
    let fileName: String

    /// Nil while there is nothing to save: no bytes on this device and no signed URL (offline, never loaded here).
    @MainActor
    init?(_ attachment: BillAttachmentTile, fileName: String) {
        if let jpeg = DecodedAttachments.jpeg(id: attachment.id, bytes: attachment.localImage) {
            source = .jpeg(jpeg)
        } else if let url = attachment.imageUrl, let remote = URL(string: url) {
            source = .remote(remote)
        } else {
            return nil
        }
        self.fileName = fileName
    }

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .jpeg) { file in
            switch file.source {
            case let .jpeg(data):
                data
            case let .remote(url):
                try await URLSession.shared.data(from: url).0
            }
        }
        .suggestedFileName { $0.fileName }
    }
}
