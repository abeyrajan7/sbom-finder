"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import "./device-details.css";

interface SoftwarePackage {
  name: string;
  version: string;
}

interface Vulnerability {
  cveId: string;
  description: string;
  severityLevel: string;
  sourceUrl?: string;
}

interface ExternalReference {
  referenceCategory: string;
  referenceType: string;
  referenceLocator: string;
}

interface DeviceDetail {
  name: string;
  manufacturer: string;
  category: string;
  operatingSystem: string;
  osVersion: string;
  kernelVersion: string;
  digitalFootprint: string;
  softwarePackages: SoftwarePackage[];
  vulnerabilities: Vulnerability[];
  externalReferences: ExternalReference[];
}

export default function DeviceDetailsContent() {
  const searchParams = useSearchParams();
  const device_id = searchParams.get("device_id");
  const [deviceDetails, setDeviceDetails] = useState<DeviceDetail | null>(null);
  const router = useRouter();

  const [openSections, setOpenSections] = useState({
    info: true,
    os: true,
    footprint: false,
    packages: false,
    vulnerabilities: false,
    externalReferences: false,
  });

  type SectionKey = keyof typeof openSections;

  const toggleSection = (key: SectionKey) => {
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  useEffect(() => {
    if (!device_id) return;

    fetch(`http://localhost:8080/api/devices/${device_id}/details`)
      .then((res) => res.json())
      .then((data) => setDeviceDetails(data))
      .catch((err) => console.error("Error fetching device details:", err));
  }, [device_id]);

  if (!deviceDetails) return <div>Loading...</div>;

  return (
    <>
      {/* Back button */}
      <section className="back-button-container">
        <button onClick={() => router.back()} className="back-button">
          ← Back
        </button>
      </section>

      <div className="device-details-container">
        {/* Section 1: Info */}
        <section className="device-section">
          <div className="section-header">
            <h2>Device Information</h2>
          </div>
          {openSections.info && (
            <>
              <p><strong>Name:</strong> {deviceDetails.name}</p>
              <p><strong>Manufacturer:</strong> {deviceDetails.manufacturer}</p>
              <p><strong>Category:</strong> {deviceDetails.category}</p>
            </>
          )}
        </section>

        {/* Section 2: OS */}
        <section className="device-section" onClick={() => toggleSection("os")}>
          <div className="section-header">
            <h2>Operating System</h2>
            <button>{openSections.os ? "−" : "+"}</button>
          </div>
          {openSections.os && (
            <>
              <p><strong>OS:</strong> {deviceDetails.operatingSystem}</p>
              <p><strong>OS Version:</strong> {deviceDetails.osVersion}</p>
              <p><strong>Kernel Version:</strong> {deviceDetails.kernelVersion}</p>
            </>
          )}
        </section>

        {/* Section 3: Digital Footprint */}
        <section className="device-section" onClick={() => toggleSection("footprint")}>
          <div className="section-header">
            <h2>Digital Footprint</h2>
            <button>{openSections.footprint ? "−" : "+"}</button>
          </div>
          {openSections.footprint && <p>{deviceDetails.digitalFootprint}</p>}
        </section>

        {/* Section 4: Software Packages */}
        <section className="device-section device-packages" onClick={() => toggleSection("packages")}>
          <div className="section-header">
            <h2>Software Packages</h2>
            <button>{openSections.packages ? "−" : "+"}</button>
          </div>
          {openSections.packages && (
            deviceDetails.softwarePackages.length > 0 ? (
              <ul className="package-grid">
                {deviceDetails.softwarePackages.map((pkg, idx) => (
                  <li key={idx}>
                    {pkg.name} =&gt; {pkg.version}
                  </li>
                ))}
              </ul>
            ) : (
              <p>Not Available</p>
            )
          )}
        </section>

        {/* Section 5: Vulnerabilities */}
        <section className="device-section" onClick={() => toggleSection("vulnerabilities")}>
          <div className="section-header">
            <h2>Vulnerabilities</h2>
            <button>{openSections.vulnerabilities ? "−" : "+"}</button>
          </div>
          {openSections.vulnerabilities && (
            <div className="vulnerability-grid">
              {deviceDetails.vulnerabilities.length > 0 ? (
                deviceDetails.vulnerabilities.map((vul, idx) => (
                  <div className="vuln-card" key={idx}>
                    <h4>{vul.cveId}</h4>
                    <p>{vul.description}</p>
                    <p><strong>Severity:</strong> {vul.severityLevel}</p>
                    {vul.sourceUrl && (
                      <a href={vul.sourceUrl} target="_blank" rel="noopener noreferrer">More Info</a>
                    )}
                  </div>
                ))
              ) : (
                <p>Not Available</p>
              )}
            </div>
          )}
        </section>

        {/* Section 6: External References */}
        <section className="device-section" onClick={() => toggleSection("externalReferences")}>
          <div className="section-header">
            <h2>External References</h2>
            <button>{openSections.externalReferences ? "−" : "+"}</button>
          </div>
          {openSections.externalReferences && (
            deviceDetails.externalReferences.length > 0 ? (
              <div className="reference-grid">
                {deviceDetails.externalReferences.map((ref, idx) => (
                  <div className="reference-card" key={idx}>
                    <p><strong>Category:</strong> {ref.referenceCategory}</p>
                    <p><strong>Type:</strong> {ref.referenceType}</p>
                    {ref.referenceLocator.startsWith("http") ? (
                      <p><strong>Link:</strong> <a href={ref.referenceLocator} target="_blank" rel="noopener noreferrer">View</a></p>
                    ) : (
                      <p><strong>Locator:</strong> {ref.referenceLocator}</p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p>Not Available</p>
            )
          )}
        </section>
      </div>
    </>
  );
}
