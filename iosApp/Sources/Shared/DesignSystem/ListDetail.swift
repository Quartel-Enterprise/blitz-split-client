import SwiftUI

#if os(iOS)
import UIKit
#endif

extension ScreenLayout {
    /// The canvas' list and detail of Grupos and Saldos on an iPad lying down (1180×820): the list in one column and
    /// what it opened beside it. Upright, in Split View and on iPhone, the detail opens over the list.
    var showsListDetailOnPad: Bool {
        #if os(iOS)
        UIDevice.current.userInterfaceIdiom == .pad && isLarge && size.width > size.height
        #else
        false
        #endif
    }

    /// The list's column beside the detail on an iPad lying down, sidebar of the tabs aside.
    static let listColumnWidth: CGFloat = 400
}

extension EnvironmentValues {
    /// The home's list has the group it opened beside it (an iPad lying down), so that group's row or card is
    /// highlighted; elsewhere the group covers the list and nothing is.
    @Entry var showsGroupBesideList = false
}

extension View {
    /// The row of what shows beside the list takes a tint of the accent, as a selected row does.
    @ViewBuilder
    func selectedRowBackground(_ isSelected: Bool) -> some View {
        if isSelected {
            listRowBackground(Palette.accent.opacity(0.16))
        } else {
            self
        }
    }
}
