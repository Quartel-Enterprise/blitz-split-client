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
    /// "bora-rachar-dados-2026-10-29.json" in Brazil's Portuguese, "blitz-split-dados-2026-10-29.json" in Portugal's and
    /// "blitz-split-data-2026-10-29.json" in any other language, on the device's local date.
    static func fileName(on date: Date = .now) -> String {
        let localization = Bundle.main.preferredLocalizations.first
        let isPortuguese = localization?.hasPrefix("pt") == true
        let brand = isPortuguese && localization != "pt-PT" ? "bora-rachar-" : "blitz-split-"
        let day = date.formatted(Date.ISO8601FormatStyle(timeZone: .current).year().month().day())
        return brand + (isPortuguese ? "dados-" : "data-") + day + ".json"
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
