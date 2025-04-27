"use client";

import Link from "next/link";
import "./dashboard.css";

export default function Dashboard() {
  return (
    <div className="dashboard-container">
      <div className="dashboard-box">
        <h1 className="dashboard-title">Welcome to SBOM Finder for Devices</h1>
        <p className="dashboard-description">
          Take control of your device security. Upload source code or SBOM files to generate detailed Software Bill of Materials (SBOMs), uncover vulnerabilities, compare devices, and explore historical insights — fast, simple, and powerful.
        </p>

        <Link href="/upload-device-source" passHref>
          <button className="upload-btn">Get Started</button>
        </Link>
      </div>
    </div>
  );
}
