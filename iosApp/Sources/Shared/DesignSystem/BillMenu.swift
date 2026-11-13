import SwiftUI

/// A bill's options without opening it first, on the group's Expenses, Home's "Latest expenses" and "All expenses":
/// Bill's own toolbar (Edit, Duplicate, Share, Delete, which asks first) plus Duplicate. The row's "⋯", its long press
/// on iPhone and iPad, and its right click on the Mac all show these.
struct BillMenuItems: View {
    let onEdit: () -> Void
    let onDuplicate: () -> Void
    let onShare: () -> Void
    let onDelete: () -> Void

    var body: some View {
        Button(editTitle, systemImage: "pencil", action: onEdit)
        Button(duplicateTitle, systemImage: "plus.square.on.square", action: onDuplicate)
        Button(shareTitle, systemImage: "square.and.arrow.up", action: onShare)
        Divider()
        Button(role: .destructive, action: onDelete) {
            BillMenuDeleteLabel()
        }
    }

    // The Mac's items that open a sheet or a dialog end in "…"
    private var editTitle: LocalizedStringKey {
        #if os(macOS)
        "Edit…"
        #else
        "Edit"
        #endif
    }

    private var duplicateTitle: LocalizedStringKey {
        #if os(macOS)
        "Duplicate…"
        #else
        "Duplicate"
        #endif
    }

    private var shareTitle: LocalizedStringKey {
        #if os(macOS)
        "Share…"
        #else
        "Share"
        #endif
    }
}

/// "Delete" in Bill's own words, "Apagar" in Portuguese, for a bill and a payment alike.
struct BillMenuDeleteLabel: View {
    var body: some View {
        Label {
            #if os(macOS)
            Text(LocalizedStringResource("Delete… (a bill)", defaultValue: "Delete…"))
            #else
            Text(BillCopy.delete)
            #endif
        } icon: {
            Image(systemName: "trash")
        }
    }
}

/// A row's "⋯", at its end, as the members' rows have it; `title` names the row for VoiceOver.
struct RowMenuButton<Items: View>: View {
    let title: String
    @ViewBuilder let items: () -> Items

    var body: some View {
        Menu {
            items()
        } label: {
            Image(systemName: "ellipsis")
                .foregroundStyle(.secondary)
                .frame(width: 32, height: 32)
                .contentShape(.rect)
                .fingerTarget(horizontalOverlap: 6, verticalOverlap: 6)
        }
        .menuStyle(.button)
        .buttonStyle(.borderless)
        .menuIndicator(.hidden)
        .accessibilityLabel(Text("Options for \(title)"))
    }
}
