import Shared
import SwiftUI

/// The words of a payment between two members, composed from the state: the shared code hands over names, flags and
/// amounts, and each platform writes them.
enum PaymentCopy {
    /// Someone no longer in the group.
    static func name(_ name: String?) -> String {
        name ?? String(localized: "Former member", bundle: .appLanguage)
    }

    /// "Pix", "Cash", "Transfer" or "Other".
    static func method(_ method: SettlementMethod) -> String {
        switch method {
        case .pix: String(localized: "Pix", bundle: .appLanguage)
        case .cash: String(localized: "Cash", bundle: .appLanguage)
        case .transfer: String(localized: "Transfer", bundle: .appLanguage)
        default: String(localized: "Other", bundle: .appLanguage)
        }
    }

    /// "Ana's payment recorded.", "Payment to Bruno recorded." when this device's user paid, or "Payment recorded."
    static func notice(_ notice: PaymentNotice) -> String {
        if notice.fromIsMe, let toName = notice.toName {
            return String(localized: "Payment to \(toName) recorded.", bundle: .appLanguage)
        }
        if !notice.fromIsMe, let fromName = notice.fromName {
            return String(localized: "\(fromName)'s payment recorded.", bundle: .appLanguage)
        }
        return String(localized: "Payment recorded.", bundle: .appLanguage)
    }

    /// A payment in the group's expenses: "Ana paid you", "You paid Bruno" or "Diego paid Bruno".
    static func title(_ row: PaymentRow) -> String {
        if row.toIsMe { return String(localized: "\(name(row.fromName)) paid you", bundle: .appLanguage) }
        if row.fromIsMe { return String(localized: "You paid \(name(row.toName))", bundle: .appLanguage) }
        return String(localized: "\(name(row.fromName)) paid \(name(row.toName))", bundle: .appLanguage)
    }

    /// Under it: "12:10 PM · Payment · Pix", or "12:10 PM · queued · Payment · Pix" before it reaches the server.
    static func meta(_ row: PaymentRow) -> String {
        let time = ExpenseFormat.time(row.paidAt)
        let when = row.isPending ? String(localized: "\(time) · queued", bundle: .appLanguage) : time
        return "\(when) · \(kindAndMethod(row))"
    }

    /// "Payment · Pix".
    static func kindAndMethod(_ row: PaymentRow) -> String {
        "\(String(localized: "Payment", bundle: .appLanguage)) · \(method(row.method))"
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
