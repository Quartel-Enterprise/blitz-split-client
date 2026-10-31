import Shared
import SwiftUI

/// The words of a payment between two members, composed from the state: the shared code hands over names, flags and
/// amounts, and each platform writes them.
enum PaymentCopy {
    /// Someone no longer in the group.
    static func name(_ name: String?) -> String {
        name ?? String(localized: "Former member")
    }

    /// "Pix", "Cash", "Transfer" or "Other".
    static func method(_ method: SettlementMethod) -> String {
        switch method {
        case .pix: String(localized: "Pix")
        case .cash: String(localized: "Cash")
        case .transfer: String(localized: "Transfer")
        default: String(localized: "Other")
        }
    }

    /// "Ana's payment recorded.", "Payment to Bruno recorded." when this device's user paid, or "Payment recorded."
    static func notice(_ notice: PaymentNotice) -> String {
        if notice.fromIsMe, let toName = notice.toName {
            return String(localized: "Payment to \(toName) recorded.")
        }
        if !notice.fromIsMe, let fromName = notice.fromName {
            return String(localized: "\(fromName)'s payment recorded.")
        }
        return String(localized: "Payment recorded.")
    }

    /// A payment in the group's expenses: "Ana paid you", "You paid Bruno" or "Diego paid Bruno".
    static func title(_ row: PaymentRow) -> String {
        if row.toIsMe { return String(localized: "\(name(row.fromName)) paid you") }
        if row.fromIsMe { return String(localized: "You paid \(name(row.toName))") }
        return String(localized: "\(name(row.fromName)) paid \(name(row.toName))")
    }

    /// Under it: "12:10 PM · Payment · Pix", or "12:10 PM · queued · Payment · Pix" before it reaches the server.
    static func meta(_ row: PaymentRow) -> String {
        let time = ExpenseFormat.time(row.paidAt)
        let when = row.isPending ? String(localized: "\(time) · queued") : time
        return "\(when) · \(kindAndMethod(row))"
    }

    /// "Payment · Pix".
    static func kindAndMethod(_ row: PaymentRow) -> String {
        "\(String(localized: "Payment")) · \(method(row.method))"
    }
}

extension View {
    /// "Ana's payment recorded." with "Undo", over the group's screen Registrar pagamento closed over.
    func recordedPaymentNotice(
        _ notice: PaymentNotice?,
        onUndo: @escaping () -> Void,
        onShown: @escaping () -> Void,
    ) -> some View {
        undoNotice(
            id: notice?.settlementId,
            text: notice.map(PaymentCopy.notice),
            onUndo: onUndo,
            onExpire: onShown,
        )
    }
}
