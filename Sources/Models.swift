import Foundation

/// The API decoder intentionally ignores unrelated response keys. JetPhotos
/// only consumes the JetPhotos image results and never requests flight data.
struct JetAPIResponse: Decodable {
    let jetPhotos: JetPhotosResult?

    enum CodingKeys: String, CodingKey {
        case jetPhotos = "JetPhotos"
    }
}

struct JetPhotosResult: Decodable {
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
