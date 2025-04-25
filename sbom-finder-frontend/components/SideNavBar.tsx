"use client";

import React from "react";
import { useRouter, usePathname } from "next/navigation";
import "./SideNavBar.css";

export default function SideNavBar() {
  const router = useRouter();
    const pathname = usePathname();
    const navItems = [
      { label: "Dashboard", path: "/dashboard" },
      { label: "Upload SBOM", path: "/upload-sbom" },
      { label: "SBOM List", path: "/sbom-list" },
      { label: "Compare SBOMs", path: "/compare-sboms" },
      { label: "SBOM Archives", path: "/device-sbom-archive" },
      { label: "Analytics", path: "/analytics" },
    ];

  return (
    <div className="nav-bar">
      <p className="nav-bar-title-text">SBOM FINDER</p>
      <p className="nav-bar-line"></p>
      <ul className="nav-list">
             {navItems.map((item) => (
                 <li
                  key={item.path}
                  className={`nav-item ${pathname === item.path ? "active" : ""}`}
                  onClick={() => router.push(item.path)}
                >
                  <span>{item.label}</span>
                </li>
              ))}
            </ul>
    </div>
  );
}
