"use client";

import React, { useEffect, useState, useRef } from "react";
import "./devices.css";
import Link from "next/link";
import { useDeviceStore } from "../../store/useDeviceStore";
import { SearchFilterBarRef } from "../../components/SearchFilterBar";
import SearchFilterBar from "../../components/SearchFilterBar";
import { usePathname } from "next/navigation";

interface Device {
  name: string;
  deviceId: number;
  manufacturer: string;
  category: string;
  operatingSystem: string;
  osVersion: string;
  kernelVersion: string;
  digitalFootprint: string;
  sbomId: number;
}

export default function DevicesPage() {
  const BASE_URL =
    process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

  const searchFilterRef = useRef<SearchFilterBarRef>(null);
  const pathname = usePathname();
  const { devices, setDevices } = useDeviceStore();

  const [showOverlay, setShowOverlay] = useState(false);
  const [overlayMessage, setOverlayMessage] = useState("Processing...");
  const [selectedDownloadId, setSelectedDownloadId] = useState<number | null>(
    null
  );
  const [showDownloadDialog, setShowDownloadDialog] = useState(false);
  const [footprintModal, setFootprintModal] = useState<{
    open: boolean;
    content: string;
  }>({
    open: false,
    content: "",
  });

  // Re-fetch data and reset filters when navigating back
  useEffect(() => {
    if (pathname === "/device-list") {
      fetchDevices();
      searchFilterRef.current?.resetFilters();
    }
  }, [pathname]);

  // Initial data fetch or when redirected with highlight param
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const highlight = params.get("highlight");

    if (highlight === "latest" || devices.length === 0) {
      fetch(`${BASE_URL}/api/devices/all`)
        .then((res) => res.json())
        .then((data) => {
          const sortedData = data.sort(
            (a: Device, b: Device) => b.sbomId - a.sbomId
          );
          setDevices(sortedData);
        })
        .catch((err) => console.error("Error fetching devices:", err));
    }
  }, []);

  // Filtered search
  const handleSearch = async (params: {
    query?: string;
    manufacturer?: string;
    operatingSystem?: string;
  }) => {
    const { query = "", manufacturer = "", operatingSystem = "" } = params;
    const res = await fetch(
      `${BASE_URL}/api/devices/search?query=${query}&manufacturer=${manufacturer}&operatingSystem=${operatingSystem}`
    );
    const data = await res.json();
    setDevices(data);
  };

  // Full list fetch
  const fetchDevices = async () => {
    try {
      const res = await fetch(`${BASE_URL}/api/devices/all`);
      const data = await res.json();
      setDevices(data);
    } catch (err) {
      console.error("Error fetching devices:", err);
    }
  };

  // Delete a device
  const handleDelete = async (deviceId: number) => {
    if (confirm("Are you sure you want to delete this SBOM?")) {
      try {
        setOverlayMessage(
          "Deletion in progress... Please do not close or switch tabs. This may take a few moments."
        );
        setShowOverlay(true);

        const response = await fetch(`${BASE_URL}/api/sboms/${deviceId}`, {
          method: "DELETE",
        });

        if (response.ok) {
          setDevices(devices.filter((d) => d.deviceId !== deviceId));
        } else {
          alert("Failed to delete SBOM.");
        }
      } catch (error) {
        console.error("Error deleting device:", error);
        alert("An error occurred while deleting.");
      } finally {
        setShowOverlay(false);
      }
    }
  };

  return (
    <div className="devices-container">
      <SearchFilterBar
        ref={searchFilterRef}
        onSearch={handleSearch}
        onReset={fetchDevices}
      />

      <div className="table-scroll-wrapper">
        <table className="devices-table">
          <thead>
            <tr>
              <th>Device Name</th>
              <th>Manufacturer</th>
              <th>Category</th>
              <th>OS</th>
              <th>OS Version</th>
              <th>Kernel Version</th>
              <th>Digital Footprint</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {devices.length === 0 ? (
              <tr>
                <td colSpan={8} className="empty-table-message">
                  No SBOM files found.
                </td>
              </tr>
            ) : (
              devices.map((device, index) => (
                <tr key={index}>
                  <td>
                    <Link
                      href={{
                        pathname: "/device-details",
                        query: { device_id: device.deviceId },
                      }}
                      className="text-blue-600 hover:underline"
                    >
                      {device.name}
                    </Link>
                  </td>
                  <td>{device.manufacturer}</td>
                  <td>{device.category}</td>
                  <td>{device.operatingSystem}</td>
                  <td>{device.osVersion}</td>
                  <td>{device.kernelVersion}</td>
                  <td>
                    <button
                      className="view-btn"
                      onClick={() =>
                        setFootprintModal({
                          open: true,
                          content: device.digitalFootprint,
                        })
                      }
                    >
                      View
                    </button>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="delete-btn"
                        onClick={() => handleDelete(device.deviceId)}
                      >
                        Delete
                      </button>
                      <button
                        className="download-btn"
                        onClick={() => {
                          setSelectedDownloadId(device.deviceId);
                          setShowDownloadDialog(true);
                        }}
                      >
                        Download SBOM
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Download format dialog */}
      {showDownloadDialog && selectedDownloadId && (
        <div className="download-dialog">
          <div className="dialog-box">
            <p>Select format to download:</p>
            <button
              className="download-btn"
              onClick={() => {
                window.open(
                  `${BASE_URL}/api/devices/download/${selectedDownloadId}?format=cyclonedx`,
                  "_blank"
                );
                setShowDownloadDialog(false);
              }}
            >
              CycloneDX
            </button>
            <button
              className="download-btn"
              onClick={() => {
                window.open(
                  `${BASE_URL}/api/devices/download/${selectedDownloadId}?format=spdx`,
                  "_blank"
                );
                setShowDownloadDialog(false);
              }}
            >
              SPDX
            </button>
            <button
              className="cancel-button"
              onClick={() => setShowDownloadDialog(false)}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Processing overlay */}
      {showOverlay && (
        <div className="upload-overlay">
          <div className="upload-overlay-content">
            <h3>{overlayMessage}</h3>
          </div>
        </div>
      )}

      {/* Digital footprint modal */}
      {footprintModal.open && (
        <div className="modal-overlay">
          <div className="modal-box">
            <h3>Digital Footprint</h3>
            <pre className="footprint-content">{footprintModal.content}</pre>
            <button
              className="close-btn"
              onClick={() => setFootprintModal({ open: false, content: "" })}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
