import SwiftUI

extension ScreenLayout {
    /// The canvas' list and detail of Grupos and Saldos lying down with an iPad's room (an iPad at 1180×820, an
    /// iPhone Duo open at 920×648): the list in one column and what it opened beside it. Upright, in a narrow Split
    /// View and on an iPhone, even a large one lying down, the detail opens over the list. The Mac has its own rules.
    var showsListDetail: Bool {
        #if os(iOS)
        isLarge && size.width > size.height
        #else
        false
        #endif
    }

    /// The list's column beside the detail lying down, sidebar of the tabs aside.
    static let listColumnWidth: CGFloat = 400
}

extension EnvironmentValues {
    /// The home's list has the group it opened beside it (an iPad lying down), so that group's row or card is
    /// highlighted; elsewhere the group covers the list and nothing is.
    @Entry var showsGroupBesideList = false
}

extension View {
    /// The row of what shows beside the list takes a tint of the accent, as a selected row does. One view either way,
    /// so picking a row never rebuilds it.
    func selectedRowBackground(_ isSelected: Bool) -> some View {
        listRowBackground(isSelected ? Palette.accent.opacity(0.16) : nil)
    }
}
