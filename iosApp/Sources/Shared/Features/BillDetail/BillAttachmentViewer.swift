import Shared
import SwiftUI
import UniformTypeIdentifiers

/// The attachment over everything, on black: ✕, "Photo 1 of 2", Download and Remove photo on top, who attached it
/// and when at the bottom. iPhone and iPad open it full screen and swipe between the attachments; the Mac opens it as
/// a Quick Look-like sheet, moving with its arrows or the ← → keys.
struct BillAttachmentViewer: View {
    let viewer: BillViewerValues

    #if os(macOS)
    @State private var exportsFile = false
    @FocusState private var isFocused: Bool
    #endif

    var body: some View {
        VStack(spacing: 0) {
            if let attachment = viewer.viewed {
                topBar(attachment)
                pages
                footer(attachment)
            } else {
                // The last one was removed: the viewer is closing
                Spacer()
            }
        }
        .background(Color.black.ignoresSafeArea())
        .environment(\.colorScheme, .dark)
        .glassToast([BillAttachmentToasts.removeError(viewer.removeErrorCount)])
        #if os(macOS)
        .frame(minWidth: 560, idealWidth: 760, minHeight: 460, idealHeight: 620)
        // The arrow keys move while the focus is in it, which it takes as it opens
        .focusable()
        .focusEffectDisabled()
        .focused($isFocused)
        .onAppear { isFocused = true }
        .onKeyPress(.leftArrow) { move(by: -1) }
        .onKeyPress(.rightArrow) { move(by: 1) }
        #endif
    }

    private func topBar(_ attachment: BillAttachmentTile) -> some View {
        HStack(spacing: 10) {
            Button("Close", systemImage: "xmark", action: viewer.onClose)
                .keyboardShortcut(.cancelAction)
            Spacer(minLength: 8)
            download(attachment)
            if attachment.canRemove, let onRemove = viewer.onRemove {
                Button("Remove photo", systemImage: "trash", role: .destructive) {
                    onRemove(attachment.id)
                }
            }
        }
        .labelStyle(.iconOnly)
        #if os(macOS)
        .buttonStyle(.borderless)
        .controlSize(.large)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        #else
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .controlSize(.large)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        #endif
        .overlay {
            Text(verbatim: BillCopy.viewerTitle(attachment, photoCount: viewer.photoCount))
                .font(.headline)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
        }
        .foregroundStyle(.white)
    }

    /// iOS: the share sheet, whose Save Image keeps it in Photos. The Mac: the save panel.
    @ViewBuilder
    private func download(_ attachment: BillAttachmentTile) -> some View {
        let label = Label("Download", systemImage: "square.and.arrow.down")
        if let file = AttachmentFile(attachment, fileName: BillCopy.attachmentFileName(attachment)) {
            #if os(macOS)
            Button {
                viewer.onDownload()
                exportsFile = true
            } label: {
                label
            }
            .help(Text("Download"))
            .fileExporter(
                isPresented: $exportsFile,
                item: file,
                contentTypes: [.jpeg],
                defaultFilename: file.fileName,
            ) { _ in }
            #else
            ShareLink(item: file, preview: SharePreview(BillCopy.viewerTitle(attachment, photoCount: viewer.photoCount))) {
                label
            }
            .onShareTap(viewer.onDownload)
            #endif
        } else {
            // Offline and never loaded here: nothing to save yet
            Button {} label: { label }
                .disabled(true)
        }
    }

    @ViewBuilder
    private var pages: some View {
        #if os(macOS)
        if let attachment = viewer.viewed {
            page(attachment)
                .id(attachment.id)
                .overlay {
                    HStack {
                        arrow(by: -1, systemImage: "chevron.left")
                        Spacer()
                        arrow(by: 1, systemImage: "chevron.right")
                    }
                    .padding(.horizontal, 12)
                }
        }
        #else
        TabView(selection: viewer.page) {
            ForEach(viewer.tiles, id: \.id) { attachment in
                page(attachment)
                    .tag(attachment.id)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        #endif
    }

    private func page(_ attachment: BillAttachmentTile) -> some View {
        AttachmentImage(attachment: attachment, contentMode: .fit) { _ in
            // Online, a nil URL is a signed URL on the way (or tried again each minute)
            if attachment.localImage == nil, attachment.imageUrl == nil, !viewer.isOnline {
                VStack(spacing: 10) {
                    Image(systemName: "icloud.slash")
                        .font(.largeTitle)
                    Text("The photo shows up once you're back online.")
                        .multilineTextAlignment(.center)
                }
                .foregroundStyle(.secondary)
                .padding(32)
            } else {
                ProgressView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: BillCopy.viewerTitle(attachment, photoCount: viewer.photoCount)))
        .accessibilityAddTraits(.isImage)
    }

    private func footer(_ attachment: BillAttachmentTile) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(verbatim: BillCopy.attached(attachment))
                .font(.subheadline.weight(.semibold))
            if let hint = hint(attachment) {
                Text(verbatim: hint)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            if attachment.isPending {
                BillQueuedLine(text: "Queued · goes up once you're back online", dotColor: Palette.pendingOnDark)
            }
        }
        .foregroundStyle(.white)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 16)
    }

    /// "Anyone in the group can remove it. Swipe to see the next one.", each half only where it holds.
    private func hint(_ attachment: BillAttachmentTile) -> String? {
        var parts: [String] = []
        if attachment.canRemove { parts.append(String(
            localized: "Anyone in the group can remove it.",
            bundle: .appLanguage,
        )) }
        if viewer.tiles.count > 1 {
            #if os(macOS)
            parts.append(String(localized: "Use the arrow keys to see the next one.", bundle: .appLanguage))
            #else
            parts.append(String(localized: "Swipe to see the next one.", bundle: .appLanguage))
            #endif
        }
        return parts.isEmpty ? nil : parts.joined(separator: WordGap.gap)
    }

    #if os(macOS)
    private func arrow(by offset: Int, systemImage: String) -> some View {
        let target = targetId(by: offset)
        return Button {
            if let target { viewer.page.wrappedValue = target }
        } label: {
            Image(systemName: systemImage)
                .font(.title2.weight(.semibold))
                .frame(width: 36, height: 36)
        }
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .disabled(target == nil)
        .opacity(target == nil ? 0 : 1)
        .accessibilityLabel(offset < 0 ? Text("Previous") : Text("Next"))
    }

    private func move(by offset: Int) -> KeyPress.Result {
        guard let target = targetId(by: offset) else { return .ignored }
        viewer.page.wrappedValue = target
        return .handled
    }

    /// The attachment before or after the one shown, nil past either end.
    private func targetId(by offset: Int) -> String? {
        guard let index = viewer.viewedIndex else { return nil }
        let target = index + offset
        return viewer.tiles.indices.contains(target) ? viewer.tiles[target].id : nil
    }
    #endif
}
