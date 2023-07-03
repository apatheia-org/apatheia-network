package org.apatheia.network.model

import org.apatheia.model.PackageDataParser

trait DefaultBytesizedParser[T]
    extends PackageDataParser[T]
    with BytesizedPackageData {}
