import Contacts
import Shared

#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The device's agenda through the Contacts framework: what the system says about the access, its own question, and
/// the people in it, read off the main actor. Only names and a number are read, and none of them leaves the device.
enum AgendaReader {
    /// The access as the shared code names it: iOS's limited access shows only the contacts picked.
    static var access: AgendaAccess {
        switch CNContactStore.authorizationStatus(for: .contacts) {
        case .notDetermined: .notAsked
        case .authorized: .granted
        #if os(iOS)
        case .limited: .limited
        #endif
        default: .denied
        }
    }

    /// Whether the agenda may be read now without asking: Contatos' search only reads it then.
    static var canRead: Bool {
        access == .granted || access == .limited
    }

    /// The system's own question; the answer comes back as the access it left.
    static func requestAccess() async -> AgendaAccess {
        _ = try? await CNContactStore().requestAccess(for: .contacts)
        return access
    }

    /// Everyone with a name, in the order the person's Contacts sort them, each with their first number.
    static func load() async -> [AgendaContact] {
        let people = await Task.detached(priority: .userInitiated) { readPeople() }.value
        return people.map { AgendaContact(id: $0.id, name: $0.name, phone: $0.phone) }
    }

    /// The app's page in Settings on iOS; on the Mac, Privacy & Security › Contacts in System Settings.
    static func openSettings() {
        #if os(iOS)
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
        #else
        guard let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_Contacts") else {
            return
        }
        NSWorkspace.shared.open(url)
        #endif
    }

    private nonisolated struct Person: Sendable {
        let id: String
        let name: String
        let phone: String?
    }

    private nonisolated static func readPeople() -> [Person] {
        let keys: [CNKeyDescriptor] = [
            CNContactFormatter.descriptorForRequiredKeys(for: .fullName),
            CNContactPhoneNumbersKey as CNKeyDescriptor,
        ]
        let request = CNContactFetchRequest(keysToFetch: keys)
        request.sortOrder = .userDefault
        var people: [Person] = []
        try? CNContactStore().enumerateContacts(with: request) { contact, _ in
            guard let name = CNContactFormatter.string(from: contact, style: .fullName)?
                .trimmingCharacters(in: .whitespacesAndNewlines),
                !name.isEmpty else { return }
            people.append(Person(id: contact.identifier, name: name, phone: contact.phoneNumbers.first?.value.stringValue))
        }
        return people
    }
}
