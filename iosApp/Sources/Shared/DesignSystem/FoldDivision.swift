import SwiftUI

/// The fold of an iPhone Duo where it crosses a view, as the iOS 27.1 SDK reports it: a division reserved region,
/// in that view's own coordinates (mirrored in right-to-left, as a SwiftUI `Layout` places), margins included. The
/// fold counts open flat too (the boards split there, "plano ou como livro"), so inactive regions are read as well:
/// `isActive` is the device partially open, a book when the fold runs down (`isVertical`) and a notebook when it
/// runs across. Nil below iOS 27.1, on the Mac and on any device without a fold, where the layouts fall back on the
/// window's size (`ScreenLayout`). Apple steers layout to these regions rather than to the hinge's angle
/// (`onHingeChange`), which is for interactions, so that one isn't read here.
nonisolated struct FoldDivision: Equatable {
    var frame: CGRect
    var isActive: Bool

    var isVertical: Bool {
        frame.height > frame.width
    }
}

extension View {
    /// Keeps `division` on the fold that crosses this view (see `FoldDivision`), read from a background the size of
    /// this view, so its frame is in the same coordinates as what this view lays out.
    func readingFoldDivision(_ division: Binding<FoldDivision?>) -> some View {
        #if os(iOS)
        background {
            GeometryReader { proxy in
                Color.clear
                    .onChange(of: foldDivision(in: proxy), initial: true) { _, newDivision in
                        division.wrappedValue = newDivision
                    }
            }
        }
        #else
        self
        #endif
    }
}

#if os(iOS)
/// The iPhone Duo's single fold, the first division over the view; the SDK has the API from iOS 27.1 only.
private func foldDivision(in proxy: GeometryProxy) -> FoldDivision? {
    guard #available(iOS 27.1, *) else { return nil }
    guard let region = proxy.reservedRegions(kind: .division, options: .includeInactive).first else { return nil }
    return FoldDivision(frame: region.frame, isActive: region.isActive)
}
#endif

extension FoldDivision {
    /// The fold where it runs down the view (an iPhone Duo open as a book, or flat lying down), the line two columns
    /// meet on; nil where it runs across or there is none.
    var verticalFrame: CGRect? {
        isVertical ? frame : nil
    }

    /// The fold where it runs across the view (an iPhone Duo open upright, as a notebook), the line a top and a
    /// bottom half meet on; nil where it runs down or there is none.
    var horizontalFrame: CGRect? {
        isVertical ? nil : frame
    }
}

/// Where a list and the column beside it meet: the iPhone Duo's fold band, kept empty (`fold`, its `verticalFrame`),
/// or a divider anywhere without one.
struct ListDetailSeparator: View {
    let fold: CGRect?

    var body: some View {
        if let fold {
            Color.clear
                .frame(width: fold.width)
        } else {
            Divider()
                .ignoresSafeArea()
        }
    }
}

extension EnvironmentValues {
    /// The iPhone Duo's fold over the whole window, safe areas included, read once by the root (`measuringWindow`), so
    /// a sheet knows which half of the window it opens in. Nil wherever `FoldDivision` is.
    @Entry var windowFold: FoldDivision?
}

extension View {
    /// A sheet the boards draw in the front half of an iPhone Duo open lying down or as a book: the leading half,
    /// with what it opened over in the other one (iOS 27's `presentationPlacement`), no wider than that half: the
    /// form sheet's size where it fits (`isForm`), a bill's (`isBill`, `ScreenLayout.billSheetSize`), else the
    /// system's. Everywhere else, the iPhone Duo upright and closed included, the system places it and it keeps that
    /// size. A sheet's sizing is decided here: the root's sheet picks `isForm` and `isBill` from its route
    /// (`AppRoute.usesFormSheet`, `usesBillSheet`) or what is pushed inside it (`growsSheetToBill`), a screen's own
    /// sheet from what it draws. SwiftUI applies it only as the sheet opens, so the root's sheet also resizes as routes
    /// are pushed inside it (`resizingSheet`). Put it on the sheet's content, as a detent.
    func foldSheetPlacement(isForm: Bool = false, isBill: Bool = false) -> some View {
        modifier(FoldSheetPlacement(isForm: isForm, isBill: isBill))
    }
}

private struct FoldSheetPlacement: ViewModifier {
    let isForm: Bool
    let isBill: Bool

    @Environment(\.windowFold) private var fold
    @Environment(\.windowLayout) private var window

    func body(content: Content) -> some View {
        #if os(iOS)
        if #available(iOS 27.0, *) {
            // One view whatever the fold, so opening or folding the device never rebuilds the sheet's content
            content
                .presentationPlacement(fold?.verticalFrame != nil ? .leading : .automatic)
                .presentationSizing(
                    FoldHalfSizing(
                        isForm: isForm,
                        size: isBill ? window.billSheetSize : nil,
                        foldStart: fold?.verticalFrame?.minX,
                    ),
                )
        } else {
            content
        }
        #else
        content
        #endif
    }
}

extension View {
    /// Grows the open sheet to a bill's size while `toBill` (Divisão padrão pushed inside Editar grupo's), and hands
    /// that size below as `grownSheetSize`, so what is pushed lays out for it from its first frame; back to its own
    /// size once it no longer does, since SwiftUI reads a sheet's `presentationSizing` only as it opens. Only the
    /// root's sheets grow; the Mac sizes a sheet by its content.
    func resizingSheet(toBill: Bool) -> some View {
        #if os(iOS)
        modifier(SheetResizing(toBill: toBill))
        #else
        self
        #endif
    }
}

extension EnvironmentValues {
    /// The size the root's sheet grows to while a route pushed inside it asks for a bill's (`resizingSheet`), which
    /// that route lays out for rather than for the size it measures while the sheet is still growing. Nil otherwise.
    @Entry var grownSheetSize: CGSize?
}

#if os(iOS)
private struct SheetResizing: ViewModifier {
    let toBill: Bool

    @Environment(\.windowFold) private var fold
    @Environment(\.windowLayout) private var window

    func body(content: Content) -> some View {
        content
            .environment(\.grownSheetSize, size)
            .background(SheetResizer(size: size))
    }

    /// A bill's size, no wider than the half before the fold, as `FoldHalfSizing` proposes it; nil for any other sheet.
    private var size: CGSize? {
        guard toBill, var size = window.billSheetSize else { return nil }
        if let foldStart = fold?.verticalFrame?.minX {
            size.width = FoldHalfSizing.width(size.width, before: foldStart)
        }
        return size
    }
}

/// The open sheet's `preferredContentSize`, which UIKit animates: `size` while there is one, and none once it goes, so
/// the sheet takes its own size again for the window it is in now, never one kept from before a rotation.
private struct SheetResizer: UIViewControllerRepresentable {
    let size: CGSize?

    func makeUIViewController(context: Context) -> Controller {
        Controller()
    }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.resize(to: size)
    }

    final class Controller: UIViewController {
        private var size: CGSize?
        /// Whether the sheet holds a size set here.
        private var isResized = false

        override func viewDidLoad() {
            super.viewDidLoad()
            view.isUserInteractionEnabled = false
            view.accessibilityElementsHidden = true
        }

        func resize(to size: CGSize?) {
            self.size = size
            apply()
        }

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply()
        }

        private func apply() {
            guard let sheet = presentedSheet else { return }
            if let size {
                sheet.preferredContentSize = size
                isResized = true
            } else if isResized {
                // None makes SwiftUI write the sheet's own sizing again, for the window as it is now
                sheet.preferredContentSize = .zero
                isResized = false
            }
        }

        /// The view controller the sheet presents, at the top of the containers this one sits in.
        private var presentedSheet: UIViewController? {
            var controller: UIViewController = self
            while let parent = controller.parent { controller = parent }
            return controller.presentingViewController == nil ? nil : controller
        }
    }
}
#endif

/// A sheet no wider than the half before the fold, its margins included, so it never runs over it; its own size
/// (a bill's, the form's, or the system's) anywhere else.
private struct FoldHalfSizing: PresentationSizing {
    let isForm: Bool
    /// The bill's size the canvas draws, nil for any other sheet.
    let size: CGSize?
    /// Where the fold starts, nil without one down the window.
    let foldStart: CGFloat?

    /// What the sheet keeps clear of the screen's edge and of the fold, on either side.
    private static let margin: CGFloat = 10

    /// `width`, the whole half without one, no wider than the half before the fold.
    static func width(_ width: CGFloat?, before foldStart: CGFloat) -> CGFloat {
        let half = foldStart - margin * 2
        return min(width ?? half, half)
    }

    func proposedSize(for root: PresentationSizingRoot, context: PresentationSizingContext) -> ProposedViewSize {
        let own = if let size {
            ProposedViewSize(size)
        } else if isForm {
            FormPresentationSizing.form.proposedSize(for: root, context: context)
        } else {
            AutomaticPresentationSizing.automatic.proposedSize(for: root, context: context)
        }
        guard let foldStart else { return own }
        return ProposedViewSize(width: Self.width(own.width, before: foldStart), height: own.height)
    }
}
