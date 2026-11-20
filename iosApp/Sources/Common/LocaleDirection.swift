import SwiftUI

extension View {
    /// Drawn in `locale`'s language and in its direction, mirrored for Arabic and Hebrew: the app's windows for Profile ›
    /// Language (`appLocale`) and the balance widget, which draws in the language the app picked.
    func drawn(in locale: Locale) -> some View {
        environment(\.locale, locale)
            .environment(
                \.layoutDirection,
                locale.language.characterDirection == .rightToLeft ? .rightToLeft : .leftToRight,
            )
    }
}
