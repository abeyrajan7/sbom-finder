"use client";

import React, { useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import "./SideNavBar.css";

export default function SideNavBar() {
  const router = useRouter();
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);

  const navItems = [
    { label: "Dashboard", path: "/dashboard" },
    { label: "Upload Device Source", path: "/upload-device-source" },
    { label: "Device List", path: "/device-list" },
    { label: "Compare Device SBOMs", path: "/compare-device-sboms" },
    { label: "SBOM Archives", path: "/device-sbom-archive" },
    { label: "Analytics", path: "/analytics" },
  ];

  const handleNavClick = (path: string) => {
    router.push(path);
    setIsOpen(false); // Close nav on mobile after navigation
  };

  return (
    <>
      {/* Hamburger Toggle Button (mobile only) */}
      {!isOpen && (
        <button
          className="nav-toggle-button"
          onClick={() => setIsOpen(true)}
          aria-label="Open navigation"
        >
          ☰
        </button>
      )}

      <div className={`nav-bar ${isOpen ? "open" : ""}`}>
        {/* Sidebar Header with Title and Close Icon */}
        <div className="top-nav-header">
          <p className="nav-bar-title-text">SBOM FINDER</p>
          <button
            className="close-button"
            onClick={() => setIsOpen(false)}
            aria-label="Close navigation"
          >
            ×
          </button>
        </div>
        <p className="nav-bar-line"></p>

        <ul className="nav-list">
          {navItems.map((item) => {
            const isActive =
              pathname.startsWith(item.path) ||
              (pathname.startsWith("/device-details") && item.path === "/device-list");

            return (
              <li
                key={item.path}
                className={`nav-item ${isActive ? "active" : ""}`}
                onClick={() => handleNavClick(item.path)}
              >
                {item.label}
              </li>
            );
          })}
        </ul>
      </div>
    </>
  );

}
