"use client";

import React, { useEffect, useState } from "react";
import "./devices.css";
import Link from "next/link";
import SearchFilterBar from "../../components/SearchFilterBar";

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
//   const BASE_URL = 'https://sbom-finder-backend.onrender.com';
  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const [devices, setDevices] = useState<Device[]>([]);
  const [openDropdown, setOpenDropdown] = useState<number | null>(null);
  const [showOverlay, setShowOverlay] = useState(false);
  const [overlayMessage, setOverlayMessage] = useState("Processing...");
  const [selectedDownloadId, setSelectedDownloadId] = useState<number | null>(null);
  const [showDownloadDialog, setShowDownloadDialog] = useState(false);
  const [footprintModal, setFootprintModal] = useState<{ open: boolean, content: string }>({
    open: false,
    content: ''
  });

  useEffect(() => {
    fetch(`${BASE_URL}/api/devices/all`)
      .then((res) => res.json())
      .then((data) => {
        const sortedData = data.sort((a: Device, b: Device) => b.sbomId - a.sbomId); // descending by sbomId
        setDevices(sortedData);
      })
      .catch((err) => console.error("Error fetching devices:", err));
  }, []);

  const handleSearch = async (params: {
    query?: string;
    manufacturer?: string;
    operatingSystem?: string;
  }) => {
    const { query = '', manufacturer = '', operatingSystem = '' } = params; // destructure here

    const res = await fetch(
      `${BASE_URL}/api/devices/search?query=${query}&manufacturer=${manufacturer}&operatingSystem=${operatingSystem}`
    );
    const data = await res.json();
    setDevices(data);
  };

  const fetchDevices = async () => {
    try {
      const res = await fetch(`${BASE_URL}/api/devices/all`);
      const data = await res.json();
      setDevices(data);
    } catch (err) {
      console.error("Error fetching devices:", err);
    }
  };

  const handleDelete = async (deviceId : number) => {
    if (confirm("Are you sure you want to delete this SBOM?")) {
      try {
          setOverlayMessage("Deletion in progress... Please do not close or switch tabs. This may take a few moments.");
          setShowOverlay(true);
          const response = await fetch(`${BASE_URL}/api/sboms/${deviceId}`, {
              method: "DELETE",
              });

        if (response.ok) {
          fetchDevices();
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

//   const toggleDropdown = (index: number) => {
//     setOpenDropdown((prev) => (prev === index ? null : index));
//   };

  return (
    <div className="devices-container">
      <SearchFilterBar onSearch={handleSearch} onReset={fetchDevices} />

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
                <td colSpan={8} style={{ textAlign: "center", padding: "1rem", color: "gray" }}>
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
                    <div className="footprint-btn-container">
                      <button
                        className="view-btn"
                        onClick={() =>
                          setFootprintModal({ open: true, content: device.digitalFootprint })
                        }
                      >
                        View
                      </button>
                    </div>
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
                        onClick={() => {
                          setSelectedDownloadId(device.deviceId);
                          setShowDownloadDialog(true);
                        }}
                        className="download-btn"
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



      {showDownloadDialog && selectedDownloadId && (
        <div className="download-dialog">
          <div className="dialog-box">
            <p>Select format to download:</p>
            <button
              className="download-btn"
              onClick={() => {
                    window.open(`${BASE_URL}/api/devices/download/${selectedDownloadId}?format=cyclonedx`, '_blank')
                    setShowDownloadDialog(false);
                }
              }
            >
              CycloneDX
            </button>
            <button
              className="download-btn"
              onClick={() =>{
                    window.open(`${BASE_URL}/api/devices/download/${selectedDownloadId}?format=spdx`, '_blank')
                    setShowDownloadDialog(false);
                }
              }
            >
              SPDX
            </button>
            <button className="cancel-button" onClick={() => setShowDownloadDialog(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}


      {showOverlay && (
        <div className="upload-overlay">
          <div className="upload-overlay-content">
            <h3>{overlayMessage}</h3>
          </div>
        </div>
      )}

  {footprintModal.open && (
          <div className="modal-overlay">
            <div className="modal-box">
              <h3>Digital Footprint</h3>
              <pre className="footprint-content">{footprintModal.content}</pre>
              <button className="close-btn" onClick={() => setFootprintModal({ open: false, content: '' })}>
                Close
              </button>
            </div>
          </div>
        )}



    </div>
  );
}
