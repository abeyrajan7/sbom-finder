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
  vulnerabilities: Vulnerability[];
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
  packages: "Packages",
  kernelVersion: "Kernel Version",
  digitalFootprint: "Digital Footprint",
  externalReferences: "External References",
};

const severityColors: { [key: string]: string } = {
  Critical: "#b71c1c",
  High: "#d32f2f",
  Medium: "#f57c00",
  Low: "#fbc02d",
  None: "#388e3c",
  Unknown: "#9e9e9e",
};

export default function CompareSbomsPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [device1Id, setDevice1Id] = useState("");
  const [device2Id, setDevice2Id] = useState("");
  const [comparisonData, setComparisonData] = useState<ComparisonResult[]>([]);
  const [visibleVulns, setVisibleVulns] = useState<{ [key: string]: boolean }>(
    {}
  );

  useEffect(() => {
    fetch("http://localhost:8080/api/devices/list")
      .then((res) => res.json())
      .then((data) => setDevices(data))
      .catch((err) => console.error("Error fetching devices list:", err));
  }, []);

  const handleCompare = () => {
    if (!device1Id || !device2Id) return;

    fetch(
      `http://localhost:8080/api/devices/compare?device1Id=${device1Id}&device2Id=${device2Id}`
    )
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
              ? data.device1?.softwarePackages || []
              : data.device1?.[field] || "Not Found",
          device2Value:
            field === "packages"
              ? data.device2?.softwarePackages || []
              : data.device2?.[field] || "Not Found",
        }));

        setComparisonData(formatted);
      })
      .catch((err) => console.error("Error comparing SBOMs:", err));
  };

  const toggleVulns = (pkgKey: string) => {
    setVisibleVulns((prev) => ({ ...prev, [pkgKey]: !prev[pkgKey] }));
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

      <button className="compare-button" onClick={handleCompare}>
        Compare
      </button>

      {comparisonData.length > 0 && (
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

                {/* Custom rendering for Packages */}
                {row.field === "packages" ? (
                  <>
                    <td>
                      {row.device1Value?.length
                        ? row.device1Value.map((pkg: any, idx: number) => (
                            <div className="package-entry" key={idx}>
                              {pkg.name} =&gt; {pkg.version}
                              {pkg.vulnerabilities &&
                                pkg.vulnerabilities.length > 0 && (
                                  <>
                                    <button
                                      className="vuln-toggle-button"
                                      onClick={() =>
                                        setComparisonData((prev) =>
                                          prev.map((r) =>
                                            r.field === "packages"
                                              ? {
                                                  ...r,
                                                  device1Value:
                                                    r.device1Value.map(
                                                      (p: any, i: number) =>
                                                        i === idx
                                                          ? {
                                                              ...p,
                                                              showVulns:
                                                                !p.showVulns,
                                                            }
                                                          : p
                                                    ),
                                                }
                                              : r
                                          )
                                        )
                                      }
                                    >
                                      {pkg.showVulns
                                        ? "Hide"
                                        : "Show Vulnerabilities"}
                                    </button>
                                    {pkg.showVulns && (
                                      <div className="vuln-tags">
                                        {pkg.vulnerabilities.map(
                                          (v: any, i: number) => (
                                            <span
                                              key={i}
                                              className={`vuln-tag ${v.severityLevel}`}
                                            >
                                              {v.cveId}
                                            </span>
                                          )
                                        )}
                                      </div>
                                    )}
                                  </>
                                )}
                            </div>
                          ))
                        : "Not Available"}
                    </td>

                    <td>
                      {row.device2Value?.length
                        ? row.device2Value.map((pkg: any, idx: number) => (
                            <div className="package-entry" key={idx}>
                              {pkg.name} =&gt; {pkg.version}
                              {pkg.vulnerabilities &&
                                pkg.vulnerabilities.length > 0 && (
                                  <>
                                    <button
                                      className="vuln-toggle-button"
                                      onClick={() =>
                                        setComparisonData((prev) =>
                                          prev.map((r) =>
                                            r.field === "packages"
                                              ? {
                                                  ...r,
                                                  device2Value:
                                                    r.device2Value.map(
                                                      (p: any, i: number) =>
                                                        i === idx
                                                          ? {
                                                              ...p,
                                                              showVulns:
                                                                !p.showVulns,
                                                            }
                                                          : p
                                                    ),
                                                }
                                              : r
                                          )
                                        )
                                      }
                                    >
                                      {pkg.showVulns
                                        ? "Hide"
                                        : "Vulnerabilities"}
                                    </button>
                                    {pkg.showVulns && (
                                      <div className="vuln-tags">
                                        {pkg.vulnerabilities.map(
                                          (v: any, i: number) => (
                                            <span
                                              key={i}
                                              className={`vuln-tag ${v.severityLevel}`}
                                            >
                                              {v.cveId}
                                            </span>
                                          )
                                        )}
                                      </div>
                                    )}
                                  </>
                                )}
                            </div>
                          ))
                        : "Not Available"}
                    </td>
                  </>
                ) : row.field === "externalReferences" ? (
                  <>
                    <td>
                      {Array.isArray(row.device1Value)
                        ? row.device1Value.length > 0
                          ? row.device1Value.map((ref: any, i: number) => (
                              <div key={i}>
                                <strong>{ref.referenceCategory}</strong>:{" "}
                                {ref.referenceType} →{" "}
                                <a
                                  href={ref.referenceLocator}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                >
                                  {ref.referenceLocator}
                                </a>
                              </div>
                            ))
                          : "Not Available"
                        : row.device1Value}
                    </td>
                    {/* Device 2 value */}
                    <td>
                      {Array.isArray(row.device2Value)
                        ? row.device2Value.length > 0
                          ? row.device2Value.map((ref: any, i: number) => (
                              <div key={i}>
                                <strong>{ref.referenceCategory}</strong>:{" "}
                                {ref.referenceType} →{" "}
                                <a
                                  href={ref.referenceLocator}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                >
                                  {ref.referenceLocator}
                                </a>
                              </div>
                            ))
                          : "Not Available"
                        : row.device2Value}
                    </td>
                  </>
                ) : (
                  <>
                    <td>{row.device1Value}</td>
                    <td>{row.device2Value}</td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
