import Foundation

/// jetapi.dev returns the registration and photo array at the JSON root:
/// { "Reg": "...", "Images": [...] }
struct JetAPIResponse: Decodable {
    let reg: String
    let images: [JetPhoto]

    enum CodingKeys: String, CodingKey {
        case reg = "Reg"
        case images = "Images"
    }
}

struct JetPhoto: Decodable, Identifiable, Hashable {
    let image: String
    let link: String
    let thumbnail: String
    let dateTaken: String
    let dateUploaded: String
    let location: String
    let photographer: String
    let aircraft: String
    let serial: String
    let airline: String

    var id: String { link }
    var thumbnailURL: URL? {
        URL(string: thumbnail.hasPrefix("http") ? thumbnail : "https:" + thumbnail)
    }
    var imageURL: URL? { URL(string: image) }

    enum CodingKeys: String, CodingKey {
        case image = "Image"
        case link = "Link"
        case thumbnail = "Thumbnail"
        case dateTaken = "DateTaken"
        case dateUploaded = "DateUploaded"
        case location = "Location"
        case photographer = "Photographer"
        case aircraft = "Aircraft"
        case serial = "Serial"
        case airline = "Airline"
    }
}
