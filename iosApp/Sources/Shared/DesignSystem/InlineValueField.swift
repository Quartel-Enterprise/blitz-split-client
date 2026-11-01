import SwiftUI

extension View {
    /// A short number field inside a person's row (a part, a percentage, an income): right-aligned, filled on iPhone
    /// and iPad, and outlined in red while what it holds is wrong.
    func inlineValueFieldStyle(width: CGFloat, isError: Bool) -> some View {
        multilineTextAlignment(.trailing)
            .monospacedDigit()
            #if os(iOS)
            .padding(.horizontal, 10)
            .frame(width: width, height: 36)
            .background(.fill.tertiary, in: .rect(cornerRadius: 10))
            .overlay {
                if isError {
                    RoundedRectangle(cornerRadius: 10).strokeBorder(Palette.negative, lineWidth: 2)
                }
            }
            #else
            .frame(width: width)
            .overlay {
                if isError {
                    RoundedRectangle(cornerRadius: 5).strokeBorder(Palette.negative, lineWidth: 1.5)
                }
            }
            #endif
    }
}
