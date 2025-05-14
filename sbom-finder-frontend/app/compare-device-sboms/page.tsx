"use client";

import { useEffect, useState } from "react";
import "./compare-sboms.css";

// Device dropdown option structure
interface Device {
  id: number;
  name: string;
}

// CVE information structure
interface Vulnerability {
  cveId: string;
  severityLevel: string;
  description: string;
  sourceUrl?: string;
}

// Software package structure, including optional supplier and CVEs
interface SoftwarePackage {
  name: string;
  version: string;
  supplierName?: string;
  vulnerabilities?: Vulnerability[];
  showVulns?: boolean;
  showSupplier?: boolean;
}

// Table row comparison result
interface ComparisonResult {
  field: string;
  device1Value: string | SoftwarePackage[];
  device2Value: string | SoftwarePackage[];
}

// External references (e.g., CPE links)
interface ExternalReference {
  referenceCategory: string;
  referenceType: string;
  referenceLocator: string;
}

// Field labels shown on the UI table
const fieldLabels: Record<string, string> = {
  name: "Device Name",
  manufacturer: "Manufacturer",
  category: "Category",
  operatingSystem: "Operating System",
  osVersion: "OS Version",
  kernelVersion: "Kernel Version",
  packages: "Packages",
  digitalFootprint: "Digital Footprint",
  externalReferences: "External References",
};

// Type guard for validating external references
function isExternalReferenceArray(arr: unknown): arr is ExternalReference[] {
  return (
    Array.isArray(arr) &&
    arr.every(
      (ref) =>
        typeof ref === "object" &&
        ref !== null &&
        "referenceCategory" in ref &&
        "referenceType" in ref &&
        "referenceLocator" in ref
    )
  );
}

// Main component
export default function CompareSbomsPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [device1Id, setDevice1Id] = useState("");
  const [device2Id, setDevice2Id] = useState("");
  const [comparisonData, setComparisonData] = useState<ComparisonResult[]>([]);

  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

  // Fetch all devices on page load
  useEffect(() => {
    fetch(`${BASE_URL}/api/devices/list`)
      .then((res) => res.json())
      .then((data) => setDevices(data))
      .catch((err) => console.error("Error fetching devices list:", err));
  }, [BASE_URL]);

  // Fetch and format comparison data for the two selected devices
  const handleCompare = () => {
    if (!device1Id || !device2Id) return;

    fetch(`${BASE_URL}/api/devices/compare?device1Id=${device1Id}&device2Id=${device2Id}`)
      .then((res) => res.json())
      .then((data) => {
        const fields = [
          "name", "manufacturer", "category", "operatingSystem",
          "osVersion", "kernelVersion", "packages", "digitalFootprint", "externalReferences"
        ];

        const formatted = fields.map((field) => ({
          field,
          device1Value:
            field === "packages"
              ? data.device1?.softwarePackages.map((pkg: SoftwarePackage) => ({ ...pkg })) || []
              : data.device1?.[field] || "Not Found",
          device2Value:
            field === "packages"
              ? data.device2?.softwarePackages.map((pkg: SoftwarePackage) => ({ ...pkg })) || []
              : data.device2?.[field] || "Not Found",
        }));

        setComparisonData(formatted);
      })
      .catch((err) => console.error("Error comparing SBOMs:", err));
  };

  // Reset form
  const handleReset = () => {
    setDevice1Id("");
    setDevice2Id("");
    setComparisonData([]);
  };

  // Toggle vulnerabilities/supplier visibility per package
  const toggleField = (
    deviceKey: "device1Value" | "device2Value",
    pkgIdx: number,
    field: "showSupplier" | "showVulns"
  ) => {
    setComparisonData((prev) =>
      prev.map((r) => {
        if (r.field === "packages" && Array.isArray(r[deviceKey])) {
          return {
            ...r,
            [deviceKey]: r[deviceKey].map((pkg: SoftwarePackage, idx: number) =>
              idx === pkgIdx ? { ...pkg, [field]: !pkg[field] } : pkg
            ),
          };
        }
        return r;
      })
    );
  };

  return (
    <div className="compare-container">
      <h1 className="compare-title">Compare SBOMs of Devices</h1>

      {/* Device selection inputs */}
      <div className="select-group">
        {[device1Id, device2Id].map((id, idx) => (
          <div className="select-box" key={idx}>
            <label>Select Device {idx + 1}</label>
            <select
              className="device-select"
              value={id}
              onChange={(e) =>
                idx === 0 ? setDevice1Id(e.target.value) : setDevice2Id(e.target.value)
              }
            >
              <option value="">Select</option>
              {devices.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.name}
                </option>
              ))}
            </select>
          </div>
        ))}
      </div>

      {/* Buttons */}
      <div className="button-group">
        <button className="compare-button" onClick={handleCompare}>Compare</button>
        <button className="reset-button" onClick={handleReset}>Reset</button>
      </div>

      {/* Comparison results */}
      {comparisonData.length > 0 && (
        <div className="compare-table-scroll-wrapper">
          <table className="comparison-table">
            <thead>
              <tr>
                <th>Field</th>
                <th>Device 1</th>
                <th>Device 2</th>
              </tr>
            </thead>
            <tbody>
              {comparisonData.map((row) => (
                <tr key={row.field}>
                  <td>{fieldLabels[row.field] || row.field}</td>

                  {/* Render complex fields like packages and externalReferences */}
                  {(row.field === "packages" || row.field === "externalReferences")
                    ? (["device1Value", "device2Value"] as const).map((key) => (
                        <td key={key}>
                          {row.field === "packages" && Array.isArray(row[key]) ? (
                            (row[key] as SoftwarePackage[]).map((pkg, idx) => (
                              <div className="package-entry" key={idx}>
                                <div className="package-item">
                                  <strong>
                                    {pkg.name || "Unknown"} ({pkg.version || "Unknown"})
                                  </strong>
                                </div>

                                {/* Supplier toggle */}
                                {pkg.supplierName &&
                                    pkg.supplierName !== "Unknown" &&
                                      pkg.supplierName !== "Unknown Supplier" && (
                                  <>
                                    <button
                                      className="vuln-toggle-button supplier-btn"
                                      onClick={() => toggleField(key, idx, "showSupplier")}
                                    >
                                      {pkg.showSupplier ? "Hide Supplier" : "Show Supplier"}
                                    </button>
                                    {pkg.showSupplier && (
                                      <div className="supplier-display">{pkg.supplierName}</div>
                                    )}
                                  </>
                                )}

                                {/* Vulnerabilities toggle */}
                                {pkg.vulnerabilities?.length > 0 && (
                                  <>
                                    <button
                                      className="vuln-toggle-button"
                                      onClick={() => toggleField(key, idx, "showVulns")}
                                    >
                                      {pkg.showVulns ? "Hide Vulnerabilities" : "Show Vulnerabilities"}
                                    </button>
                                    {pkg.showVulns && (
                                      <div className="vuln-tags">
                                        {pkg.vulnerabilities.map((v, i) => (
                                          <span
                                            key={i}
                                            className={`vuln-tag ${v.severityLevel}`}
                                          >
                                            {v.cveId}
                                          </span>
                                        ))}
                                      </div>
                                    )}
                                  </>
                                )}
                              </div>
                            ))
                          ) : row.field === "externalReferences" && isExternalReferenceArray(row[key]) ? (
                            (row[key] as ExternalReference[]).map((ref, i) => (
                              <div key={i}>
                                <strong>{ref.referenceCategory}</strong>: {ref.referenceType} →{" "}
                                <a href={ref.referenceLocator} target="_blank" rel="noopener noreferrer">
                                  {ref.referenceLocator}
                                </a>
                              </div>
                            ))
                          ) : (
                            <span>Not Available</span>
                          )}
                        </td>
                      ))
                    : (["device1Value", "device2Value"] as const).map((key) => (
                        <td key={key}>{String(row[key] ?? "Not Available")}</td>
                      ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
