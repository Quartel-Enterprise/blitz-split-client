import CoreTransferable
import SwiftUI
import UniformTypeIdentifiers

#if os(macOS)
import AppKit

typealias AttachmentPlatformImage = NSImage
#else
import UIKit

typealias AttachmentPlatformImage = UIImage
#endif

/// The bytes behind a bill's attachment: a photo added on this device and not up yet comes as a
/// `data:image/jpeg;base64,…` URI, which AsyncImage doesn't load, and one already up as an https signed URL.
nonisolated enum AttachmentSource {
    /// The JPEG inside a data URI, nil for any other URL.
    static func dataUriBytes(_ url: String) -> Data? {
        guard url.hasPrefix("data:"), let comma = url.firstIndex(of: ",") else { return nil }
        return Data(base64Encoded: String(url[url.index(after: comma)...]), options: .ignoreUnknownCharacters)
    }

    /// The image's bytes, decoded from the data URI or downloaded from the signed URL.
    static func bytes(_ url: String) async throws -> Data {
        if url.hasPrefix("data:") {
            guard let data = dataUriBytes(url) else { throw URLError(.cannotDecodeContentData) }
            return data
        }
        guard let remote = URL(string: url) else { throw URLError(.badURL) }
        let (data, _) = try await URLSession.shared.data(from: remote)
        return data
    }
}

/// Why: a data URI is decoded once, not on every redraw of the strip or the viewer; the URI itself is the key, since
/// the photo it holds never changes.
private enum DecodedAttachments {
    static let cache = NSCache<NSString, AttachmentPlatformImage>()

    static func image(_ url: String) -> AttachmentPlatformImage? {
        if let cached = cache.object(forKey: url as NSString) { return cached }
        guard let data = AttachmentSource.dataUriBytes(url), let image = AttachmentPlatformImage(data: data) else {
            return nil
        }
        cache.setObject(image, forKey: url as NSString)
        return image
    }
}

/// A bill's photo, cropped to fill a tile or fit in the viewer; `placeholder` while it loads, when it fails, and for
/// a photo never loaded on this device while offline (a nil URL).
struct AttachmentImage<Placeholder: View>: View {
    let url: String?
    let contentMode: ContentMode
    @ViewBuilder let placeholder: () -> Placeholder

    var body: some View {
        if let url, url.hasPrefix("data:") {
            if let image = DecodedAttachments.image(url) {
                drawn(image: platformImage(image))
            } else {
                placeholder()
            }
        } else if let url, let remote = URL(string: url) {
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
    let url: String
    let fileName: String

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .jpeg) { file in
            try await AttachmentSource.bytes(file.url)
        }
        .suggestedFileName { $0.fileName }
    }
}
