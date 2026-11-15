import SwiftUI
import UniformTypeIdentifiers

/// The JSON the shared ViewModels export ("Export my data"), as a document the save sheet writes.
/// Nonisolated, since the exporter writes the file off the main actor.
nonisolated struct JSONExportDocument: FileDocument {
    static let readableContentTypes: [UTType] = [.json]

    let json: String

    init(json: String) {
        self.json = json
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              let json = String(data: data, encoding: .utf8)
        else { throw CocoaError(.fileReadCorruptFile) }
        self.json = json
    }

    func fileWrapper(configuration _: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(json.utf8))
    }
}

enum DataExport {
    /// Whether the app draws in a Portuguese, as Profile › Language picked it (not the launch language), and its
    /// whole `.lproj` name ("pt-BR", "pt-PT"), which the file names below read the brand and the words from.
    static var language: (isPortuguese: Bool, isBrazil: Bool) {
        let identifier = Bundle.appLanguageIdentifier
        let isPortuguese = identifier?.hasPrefix("pt") == true
        return (isPortuguese, isPortuguese && identifier != "pt-PT")
    }

    /// "bora-rachar-dados-2026-10-29.json" in Brazil's Portuguese, "blitz-split-dados-2026-10-29.json" in Portugal's and
    /// "blitz-split-data-2026-10-29.json" in any other language, on the device's local date.
    static func fileName(on date: Date = .now) -> String {
        let language = Self.language
        let day = date.formatted(Date.ISO8601FormatStyle(timeZone: .current).year().month().day())
        return (language.isBrazil ? "bora-rachar-" : "blitz-split-") + (language.isPortuguese ? "dados-" : "data-") + day + ".json"
    }
}

extension View {
    /// Opens the save sheet (the save panel on the Mac) once the ViewModel hands over the JSON, and
    /// forgets it when the sheet closes, saved or not.
    func dataExporter(json: Binding<String?>) -> some View {
        fileExporter(
            isPresented: Binding(
                get: { json.wrappedValue != nil },
                set: { isPresented in
                    if !isPresented { json.wrappedValue = nil }
                },
            ),
            document: json.wrappedValue.map(JSONExportDocument.init(json:)),
            contentType: .json,
            defaultFilename: DataExport.fileName(),
        ) { _ in
            json.wrappedValue = nil
        }
    }
}
