"use client";

import React from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
// import dashboardIcon from "@/public/assets/icons/dashboard.png";
import "./SideNavBar.css";

export default function SideNavBar() {
  const router = useRouter();

  return (
    <div className="nav-bar">
      <p className="nav-bar-title-text">SBOM FINDER</p>
      <p className="nav-bar-line"></p>
      <ul className="nav-list">
        <li className="nav-item" onClick={() => router.push("/dashboard")}>
          {/* <Image src={dashboardIcon} alt="Dashboard" className="nav-icon" /> */}
          <span>Dashboard</span>
        </li>

        {/* Upload SBOM */}
        <li
          className="nav-item"
          onClick={() => {
            console.log("Navigating to /upload-sbom"); // Debugging log
            router.push("/upload-sbom");
          }}
        >
          {/* <Image src={dashboardIcon} alt="Upload SBOM" className="nav-icon" /> */}
          <span>Upload SBOM</span>
        </li>

        {/* SBOM List */}
        <li className="nav-item" onClick={() => router.push("/sbom-list")}>
          {/* <Image src={dashboardIcon} alt="SBOM List" className="nav-icon" /> */}
          <span>SBOM List</span>
        </li>

        <li className="nav-item" onClick={() => router.push("/compare-sboms")}>
          <span>Compare SBOMs</span>
        </li>
      </ul>
    </div>
  );
}
