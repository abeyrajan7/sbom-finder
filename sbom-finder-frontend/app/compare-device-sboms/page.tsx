"use client";

import { useEffect, useState } from "react";
import "./compare-sboms.css";

interface Device {
  id: number;
  name: string;
}

interface Vulnerability {
  cveId: string;
  severityLevel: string;
  description: string;
  sourceUrl?: string;
}

interface SoftwarePackage {
  name: string;
  version: string;
  supplierName?: string;
  vulnerabilities?: Vulnerability[];
  showVulns?: boolean;
  showSupplier?: boolean;
}

interface ComparisonResult {
  field: string;
  device1Value: string | SoftwarePackage[];
  device2Value: string | SoftwarePackage[];
}

const fieldLabels: { [key: string]: string } = {
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

type ExternalReference = {
  referenceCategory: string;
  referenceType: string;
  referenceLocator: string;
};

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

export default function CompareSbomsPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [device1Id, setDevice1Id] = useState("");
  const [device2Id, setDevice2Id] = useState("");
  const [comparisonData, setComparisonData] = useState<ComparisonResult[]>([]);
  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

  useEffect(() => {
    fetch(`${BASE_URL}/api/devices/list`)
      .then((res) => res.json())
      .then((data) => setDevices(data))
      .catch((err) => console.error("Error fetching devices list:", err));
  }, [BASE_URL]);

  const handleCompare = () => {
    if (!device1Id || !device2Id) return;

    fetch(`${BASE_URL}/api/devices/compare?device1Id=${device1Id}&device2Id=${device2Id}`)
      .then((res) => res.json())
      .then((data) => {
        const fields = [
          "name",
          "manufacturer",
          "category",
          "operatingSystem",
          "osVersion",
          "kernelVersion",
          "packages",
          "digitalFootprint",
          "externalReferences",
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

  const handleReset = () => {
    setDevice1Id("");
    setDevice2Id("");
    setComparisonData([]);
  };

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

      <div className="select-group">
        <div className="select-box">
          <label>Select Device 1</label>
          <select
            className="device-select"
            onChange={(e) => setDevice1Id(e.target.value)}
            value={device1Id}
          >
            <option value="">Select</option>
            {devices.map((device) => (
              <option key={device.id} value={device.id}>
                {device.name}
              </option>
            ))}
          </select>
        </div>

        <div className="select-box">
          <label>Select Device 2</label>
          <select
            className="device-select"
            onChange={(e) => setDevice2Id(e.target.value)}
            value={device2Id}
          >
            <option value="">Select</option>
            {devices.map((device) => (
              <option key={device.id} value={device.id}>
                {device.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="button-group">
        <button className="compare-button" onClick={handleCompare}>Compare</button>
        <button className="reset-button" onClick={handleReset}>Reset</button>
      </div>

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

                  {row.field === "packages"
                    ? (["device1Value", "device2Value"] as const).map((key) => (
                        <td key={key}>
                          {Array.isArray(row[key]) && row[key].length > 0 ? (
                            (row[key] as SoftwarePackage[]).map((pkg, idx) => (
                              <div className="package-entry" key={idx}>
                                <div className="package-item">
                                  <strong>
                                    {pkg.name || "Unknown"}
                                    {pkg.version && pkg.version !== "Unknown"
                                      ? ` (>=${pkg.version})`
                                      : " (Unknown Version)"}
                                  </strong>
                                </div>
                                <div className="button-row">
                                  {pkg.supplierName && pkg.supplierName !== "Unknown" && (
                                    <>
                                      <button
                                        className="vuln-toggle-button supplier-btn"
                                        onClick={() =>
                                          toggleField(
                                            key,
                                            idx,
                                            "showSupplier"
                                          )
                                        }
                                      >
                                        {pkg.showSupplier ? "Hide Supplier" : "Show Supplier"}
                                      </button>
                                      {pkg.showSupplier && (
                                        <div className="supplier-display">{pkg.supplierName}</div>
                                      )}
                                    </>
                                  )}

                                  {pkg.vulnerabilities && pkg.vulnerabilities.length > 0 && (
                                    <>
                                      <button
                                        className="vuln-toggle-button"
                                        onClick={() =>
                                          toggleField(
                                            key,
                                            idx,
                                            "showVulns"
                                          )
                                        }
                                      >
                                        {pkg.showVulns
                                          ? "Hide Vulnerabilities"
                                          : "Show Vulnerabilities"}
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
                              </div>
                            ))
                          ) : (
                            "Not Available"
                          )}
                        </td>
                      ))
                    : row.field === "externalReferences"
                    ? (["device1Value", "device2Value"] as const).map((key) => (
                        <td key={key}>
                          {isExternalReferenceArray(row[key]) ? (
                            row[key].map((ref, i) => (
                              <div key={i}>
                                <strong>{ref.referenceCategory}</strong>: {ref.referenceType} → {" "}
                                <a
                                  href={ref.referenceLocator}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                >
                                  {ref.referenceLocator}
                                </a>
                              </div>
                            ))
                          ) : (
                            <span>
                              {typeof row[key] === "string" ? row[key] : "Not Available"}
                            </span>
                          )}
                        </td>
                      ))
                    : (["device1Value", "device2Value"] as const).map((key) => (
                        <td key={key}>
                          {Array.isArray(row[key])
                            ? "Not Available"
                            : row[key]}
                        </td>
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
