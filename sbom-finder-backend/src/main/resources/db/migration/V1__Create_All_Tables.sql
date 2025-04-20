-- Create SBOM Files Table (No Change)
CREATE TABLE sbom_files (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    spdx_version TEXT NOT NULL,
    data_license TEXT NOT NULL,
    document_namespace TEXT NOT NULL,
    created TIMESTAMP NOT NULL,
    creator_organization TEXT,
    creator_tool TEXT
);

-- Update Devices Table (Added `kernel_version` and `digital_footprint`)
CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    sbom_id BIGINT REFERENCES sbom_files(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    manufacturer TEXT,
    category TEXT,
    operating_system TEXT,
    os_version TEXT, -- New Field: Store OS version
    kernel_version TEXT, -- New Field: Store Kernel version
    digital_footprint TEXT -- New Field: Store Digital Footprint
);

-- Update Software Packages Table (Added `component_type`)
CREATE TABLE software_packages (
    id BIGSERIAL PRIMARY KEY,
    sbom_id BIGINT REFERENCES sbom_files(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    version TEXT,
    supplier TEXT,
    download_location TEXT,
    license_declared TEXT,
    license_concluded TEXT,
    source_info TEXT,
    copyright_text TEXT,
    component_type TEXT -- New Field: OS, Kernel, or Application
);

-- Create External References Table (No Change)
CREATE TABLE external_references (
    id BIGSERIAL PRIMARY KEY,
    sbom_id BIGINT REFERENCES sbom_files(id) ON DELETE CASCADE,
    reference_category TEXT,
    reference_type TEXT,
    reference_locator TEXT
);
