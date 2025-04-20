"use client";

import Link from "next/link";
import "./dashboard.css";

export default function Dashboard() {
  return (
    <div className="dashboard-container">
      <div className="dashboard-box">
        <h1 className="dashboard-title">Welcome to SBOM Finder</h1>
        <p className="dashboard-description">
          Analyze, compare, and manage Software Bill of Materials (SBOMs) for your devices.
          Easily upload SPDX, CycloneDX, or CPE formatted files and get insights into package
          vulnerabilities and digital footprints.
        </p>

        <Link href="/upload-sbom" passHref>
          <button className="upload-btn">Upload SBOM File</button>
        </Link>
      </div>
    </div>
  );
}
