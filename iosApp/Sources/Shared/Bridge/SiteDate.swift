import Foundation

/// A day the site wrote (a legal text's last change, a version's release), long ("November 14, 2026") in the
/// device's own format. The site counts in the Gregorian calendar, whichever one the device counts in.
func longSiteDate(year: Int32, month: Int32, day: Int32) -> String {
    let components = DateComponents(year: Int(year), month: Int(month), day: Int(day))
    guard let date = Calendar(identifier: .gregorian).date(from: components) else { return "" }
    return date.formatted(date: .long, time: .omitted)
}
