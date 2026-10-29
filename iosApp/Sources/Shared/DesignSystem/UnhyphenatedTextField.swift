import SwiftUI

/// An address has no word to break between, so a line ends at whichever character fills it, and with
/// hyphenation off no hyphen is drawn there.
private func unhyphenatedParagraph() -> NSParagraphStyle {
    let paragraph = NSMutableParagraphStyle()
    paragraph.lineBreakMode = .byCharWrapping
    paragraph.hyphenationFactor = 0
    paragraph.usesDefaultHyphenation = false
    return paragraph
}

#if os(iOS)
import UIKit

/// A multi-line field that wraps at any character and never hyphenates. SwiftUI's vertical `TextField`
/// hyphenates a long word where it wraps, on iOS and on the Mac, so a Bitcoin address showed a hyphen that
/// isn't part of it. This one draws the text with `UITextView` (`NSTextView` on the Mac) instead:
/// monospaced body, growing with the text from the first to the last of `lines` and then scrolling, with
/// Return submitting instead of starting a new line.
///
/// It only draws and reports the text: whoever owns `text` decides what it holds, and a `text` that
/// changes from outside replaces the field's own and puts the cursor at the end. It has no placeholder of
/// its own: the owner lays one over it, in `placeholderColor`, while the text is empty.
struct UnhyphenatedTextField: UIViewRepresentable {
    static let placeholderColor = Color(uiColor: .placeholderText)

    @Binding var text: String
    let isFocused: FocusState<Bool>.Binding
    let accessibilityLabel: String
    let lines: ClosedRange<Int>
    let onSubmit: () -> Void
    private var keyboardType: UIKeyboardType = .default
    private var textContentType: UITextContentType?

    init(
        text: Binding<String>,
        isFocused: FocusState<Bool>.Binding,
        accessibilityLabel: String,
        lines: ClosedRange<Int>,
        onSubmit: @escaping () -> Void,
    ) {
        _text = text
        self.isFocused = isFocused
        self.accessibilityLabel = accessibilityLabel
        self.lines = lines
        self.onSubmit = onSubmit
    }

    /// The keyboard and what the text is, as `.keyboardType` and `.textContentType` give a TextField.
    func keyboard(_ type: UIKeyboardType, contentType: UITextContentType?) -> Self {
        var field = self
        field.keyboardType = type
        field.textContentType = contentType
        return field
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.delegate = context.coordinator
        view.backgroundColor = .clear
        // The text starts where a TextField's does, so the row's insets and the placeholder line up
        view.textContainerInset = .zero
        view.textContainer.lineFragmentPadding = 0
        view.textContainer.lineBreakMode = .byCharWrapping
        view.autocorrectionType = .no
        view.autocapitalizationType = .none
        view.spellCheckingType = .no
        view.smartQuotesType = .no
        view.smartDashesType = .no
        view.smartInsertDeleteType = .no
        view.returnKeyType = .done
        view.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        let coordinator = context.coordinator
        coordinator.parent = self

        let font = Self.font(for: context.environment.dynamicTypeSize)
        let fontChanged = font != coordinator.font
        if fontChanged {
            coordinator.font = font
        }
        let attributes = Self.attributes(font: font)
        // Never under a composition the keyboard is still marking
        if view.markedTextRange == nil, fontChanged || view.text != text {
            let isReplaced = view.text != text
            let selection = view.selectedRange
            view.attributedText = NSAttributedString(string: text, attributes: attributes)
            if isReplaced {
                view.selectedRange = NSRange(location: (text as NSString).length, length: 0)
                view.scrollRangeToVisible(view.selectedRange)
            } else {
                view.selectedRange = selection
            }
        }
        view.typingAttributes = attributes

        if view.keyboardType != keyboardType || view.textContentType != textContentType {
            view.keyboardType = keyboardType
            view.textContentType = textContentType
            if view.isFirstResponder { view.reloadInputViews() }
        }
        view.accessibilityLabel = accessibilityLabel

        // The FocusState drives the first responder too, a turn later: UIKit must not be asked to
        // change it in the middle of SwiftUI's update
        let wantsFocus = isFocused.wrappedValue
        if wantsFocus != view.isFirstResponder {
            DispatchQueue.main.async {
                guard view.window != nil, wantsFocus != view.isFirstResponder else { return }
                if wantsFocus {
                    view.becomeFirstResponder()
                } else {
                    view.resignFirstResponder()
                }
            }
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView view: UITextView, context: Context) -> CGSize? {
        guard let width = proposal.width, width.isFinite, width > 0 else { return nil }
        let lineHeight = Self.font(for: context.environment.dynamicTypeSize).lineHeight
        let minHeight = lineHeight * CGFloat(lines.lowerBound)
        let maxHeight = lineHeight * CGFloat(lines.upperBound)
        let fitting = view.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude)).height
        view.isScrollEnabled = fitting > maxHeight
        return CGSize(width: width, height: ceil(min(max(fitting, minHeight), maxHeight)))
    }

    /// `.body.monospaced()`, at the Dynamic Type size SwiftUI lays the screen out with.
    private static func font(for size: DynamicTypeSize) -> UIFont {
        let traits = UITraitCollection(preferredContentSizeCategory: UIContentSizeCategory(size))
        let body = UIFont.preferredFont(forTextStyle: .body, compatibleWith: traits)
        let descriptor = body.fontDescriptor.withDesign(.monospaced) ?? body.fontDescriptor
        return UIFont(descriptor: descriptor, size: 0)
    }

    private static func attributes(font: UIFont) -> [NSAttributedString.Key: Any] {
        [.font: font, .foregroundColor: UIColor.label, .paragraphStyle: unhyphenatedParagraph()]
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: UnhyphenatedTextField
        var font: UIFont?

        init(parent: UnhyphenatedTextField) {
            self.parent = parent
        }

        func textViewDidChange(_ textView: UITextView) {
            parent.text = textView.text
        }

        func textView(
            _ textView: UITextView,
            shouldChangeTextIn range: NSRange,
            replacementText text: String,
        ) -> Bool {
            guard text == "\n" else { return true }
            parent.onSubmit()
            return false
        }

        func textViewDidBeginEditing(_ textView: UITextView) {
            if !parent.isFocused.wrappedValue { parent.isFocused.wrappedValue = true }
        }

        func textViewDidEndEditing(_ textView: UITextView) {
            if parent.isFocused.wrappedValue { parent.isFocused.wrappedValue = false }
        }
    }
}

#elseif os(macOS)
import AppKit

/// The Mac's `UnhyphenatedTextField`, over `NSTextView`: the same field as on iOS, where its role is told.
struct UnhyphenatedTextField: NSViewRepresentable {
    static let placeholderColor = Color(nsColor: .placeholderTextColor)

    @Binding var text: String
    let isFocused: FocusState<Bool>.Binding
    let accessibilityLabel: String
    let lines: ClosedRange<Int>
    let onSubmit: () -> Void

    /// `.body.monospaced()`: the Mac has no Dynamic Type, so the size is the body's.
    private static let font: NSFont = {
        let body = NSFont.preferredFont(forTextStyle: .body)
        let descriptor = body.fontDescriptor.withDesign(.monospaced) ?? body.fontDescriptor
        return NSFont(descriptor: descriptor, size: body.pointSize)
            ?? .monospacedSystemFont(ofSize: body.pointSize, weight: .regular)
    }()

    private static let attributes: [NSAttributedString.Key: Any] = [
        .font: font,
        .foregroundColor: NSColor.labelColor,
        .paragraphStyle: unhyphenatedParagraph(),
    ]

    private static let lineHeight = NSLayoutManager().defaultLineHeight(for: font)

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeNSView(context: Context) -> NSScrollView {
        let textView = FocusReportingTextView(frame: .zero)
        textView.delegate = context.coordinator
        context.coordinator.observeEdits(of: textView)
        textView.onFocusChange = { [weak coordinator = context.coordinator] isFocused in
            coordinator?.focusChanged(isFocused)
        }
        textView.drawsBackground = false
        textView.isRichText = false
        textView.importsGraphics = false
        textView.allowsUndo = true
        textView.isContinuousSpellCheckingEnabled = false
        textView.isGrammarCheckingEnabled = false
        textView.isAutomaticSpellingCorrectionEnabled = false
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.isAutomaticTextCompletionEnabled = false
        textView.isAutomaticLinkDetectionEnabled = false
        textView.isAutomaticDataDetectionEnabled = false
        textView.smartInsertDeleteEnabled = false
        textView.inlinePredictionType = .no
        // The text starts where a TextField's does, so the row's insets and the placeholder line up
        textView.textContainerInset = .zero
        textView.textContainer?.lineFragmentPadding = 0
        textView.textContainer?.lineBreakMode = .byCharWrapping
        textView.textContainer?.widthTracksTextView = true
        textView.textContainer?.containerSize = NSSize(width: 0, height: CGFloat.greatestFiniteMagnitude)
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [.width]
        textView.minSize = .zero
        textView.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude)
        textView.font = Self.font
        textView.textColor = .labelColor
        textView.defaultParagraphStyle = unhyphenatedParagraph()
        textView.typingAttributes = Self.attributes

        let scrollView = NSScrollView()
        scrollView.drawsBackground = false
        scrollView.borderType = .noBorder
        scrollView.hasVerticalScroller = true
        scrollView.hasHorizontalScroller = false
        scrollView.autohidesScrollers = true
        scrollView.documentView = textView
        scrollView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return scrollView
    }

    func updateNSView(_ scrollView: NSScrollView, context: Context) {
        let coordinator = context.coordinator
        coordinator.parent = self
        guard let textView = scrollView.documentView as? FocusReportingTextView else { return }
        textView.setAccessibilityLabel(accessibilityLabel)

        // Never under a composition the input method is still marking
        if !textView.hasMarkedText(), textView.string != text {
            // Through shouldChangeText and didChangeText, so undo keeps up with the replaced text
            coordinator.isReplacing = true
            let all = NSRange(location: 0, length: (textView.string as NSString).length)
            if textView.shouldChangeText(in: all, replacementString: text) {
                textView.textStorage?.replaceCharacters(
                    in: all,
                    with: NSAttributedString(string: text, attributes: Self.attributes),
                )
                textView.didChangeText()
            }
            coordinator.isReplacing = false
            textView.setSelectedRange(NSRange(location: (text as NSString).length, length: 0))
            textView.scrollRangeToVisible(textView.selectedRange())
        }
        textView.typingAttributes = Self.attributes

        // The FocusState drives the first responder too, a turn later: AppKit must not be asked to
        // change it in the middle of SwiftUI's update
        let wantsFocus = isFocused.wrappedValue
        if wantsFocus != textView.isFocused {
            DispatchQueue.main.async {
                guard let window = textView.window, wantsFocus != textView.isFocused else { return }
                window.makeFirstResponder(wantsFocus ? textView : nil)
            }
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, nsView: NSScrollView, context: Context) -> CGSize? {
        guard let width = proposal.width, width.isFinite, width > 0 else { return nil }
        let minHeight = Self.lineHeight * CGFloat(lines.lowerBound)
        let maxHeight = Self.lineHeight * CGFloat(lines.upperBound)
        let fitting = NSAttributedString(string: text, attributes: Self.attributes)
            .boundingRect(
                with: NSSize(width: width, height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading],
            )
            .height
        return CGSize(width: width, height: ceil(min(max(fitting, minHeight), maxHeight)))
    }

    final class Coordinator: NSObject, NSTextViewDelegate {
        var parent: UnhyphenatedTextField
        /// Set while `updateNSView` puts the owner's text in, which must not be reported back as an edit.
        var isReplacing = false
        private weak var textView: NSTextView?

        init(parent: UnhyphenatedTextField) {
            self.parent = parent
        }

        /// Through the text storage, which reports every change to the characters, undo and redo
        /// included, that `textDidChange` leaves out. Not as its delegate: TextKit 2's content storage
        /// already is.
        func observeEdits(of textView: NSTextView) {
            self.textView = textView
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(charactersDidChange(_:)),
                name: NSTextStorage.didProcessEditingNotification,
                object: textView.textStorage,
            )
        }

        @objc private func charactersDidChange(_ notification: Notification) {
            guard !isReplacing,
                  let storage = notification.object as? NSTextStorage,
                  storage.editedMask.contains(.editedCharacters),
                  let textView
            else { return }
            parent.text = textView.string
        }

        // Return submits and Tab moves on, as in a TextField
        func textView(_ textView: NSTextView, doCommandBy selector: Selector) -> Bool {
            switch selector {
            case #selector(NSResponder.insertNewline(_:)):
                parent.onSubmit()
            case #selector(NSResponder.insertTab(_:)):
                textView.window?.selectNextKeyView(nil)
            case #selector(NSResponder.insertBacktab(_:)):
                textView.window?.selectPreviousKeyView(nil)
            default:
                return false
            }
            return true
        }

        func focusChanged(_ isFocused: Bool) {
            if parent.isFocused.wrappedValue != isFocused { parent.isFocused.wrappedValue = isFocused }
        }
    }

    /// Tells when it becomes or stops being the first responder: an NSTextView only reports editing
    /// once the text changes, not when it is clicked into.
    final class FocusReportingTextView: NSTextView {
        var onFocusChange: ((Bool) -> Void)?

        var isFocused: Bool {
            window?.firstResponder === self
        }

        override func becomeFirstResponder() -> Bool {
            let became = super.becomeFirstResponder()
            if became { onFocusChange?(true) }
            return became
        }

        override func resignFirstResponder() -> Bool {
            let resigned = super.resignFirstResponder()
            if resigned { onFocusChange?(false) }
            return resigned
        }
    }
}
#endif
