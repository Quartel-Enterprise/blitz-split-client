import SwiftUI

/// The 12 recovery words, numbered so they are written down in order. They read row by row, the order the restore
/// screen's fields take them in.
struct RecoveryWordsGrid: View {
    let words: [String]
    var columnCount = 3

    var body: some View {
        Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 8) {
            ForEach(Array(stride(from: 0, to: words.count, by: columnCount)), id: \.self) { start in
                GridRow {
                    ForEach(start ..< min(start + columnCount, words.count), id: \.self) { index in
                        HStack(spacing: 6) {
                            Text(verbatim: "\(index + 1)")
                                .font(.subheadline)
                                .monospacedDigit()
                                .foregroundStyle(.secondary)
                                .frame(minWidth: 20, alignment: .trailing)
                            Text(verbatim: words[index])
                                .font(.system(.body, design: .monospaced).weight(.semibold))
                                .lineLimit(1)
                                .fixedSize()
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
            }
        }
        .textSelection(.enabled)
    }
}
