"use client";

import React, { useState, useRef } from "react";
import "./upload.css";
import { useRouter } from "next/navigation";

export default function UploadSBOMPage() {
  const BASE_URL =
    process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const [file, setFile] = useState<File | null>(null);
  const [manufacturer, setManufacturer] = useState("");
  const [category, setCategory] = useState("");
  const [operatingSystem, setOperatingSystem] = useState("");
  const [osVersion, setOsVersion] = useState("");
  const [kernelVersion, setKernelVersion] = useState("");
  const [deviceName, setDeviceName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showOverlay, setShowOverlay] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const router = useRouter();

  const handleBeforeUnload = (event: BeforeUnloadEvent) => {
    event.preventDefault();
    event.returnValue =
      "Upload is in progress. Are you sure you want to leave?";
  };

  const handleUpload = async () => {
    setShowOverlay(true);
    window.addEventListener("beforeunload", handleBeforeUnload);
    setError(null);

    if (!file) {
      setError("Please select a file to upload.");
      return;
    }

    if (!category || !deviceName) {
      setError("Please fill all required fields.");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("category", category);
    formData.append("deviceName", deviceName);
    formData.append("manufacturer", manufacturer || "Unknown Manufacturer");
    formData.append("operatingSystem", operatingSystem || "Unknown OS");
    formData.append("osVersion", osVersion || "Unknown Version");
    formData.append("kernelVersion", kernelVersion || "Unknown Kernel");

    try {
      const response = await fetch(`${BASE_URL}/api/sboms/upload-source`, {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        resetForm();
        router.push("/device-list?highlight=latest");
      } else {
        const errorText = await response.text();
        setError(errorText || "Upload failed.");
      }
    } catch (err) {
      console.error("Upload Error:", err);
      setError("Upload failed.");
    } finally {
      setShowOverlay(false);
      window.removeEventListener("beforeunload", handleBeforeUnload);
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFile(null);
    setManufacturer("");
    setOperatingSystem("");
    setOsVersion("");
    setKernelVersion("");
    setDeviceName("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h2 className="upload-title">Upload Source File</h2>

        {/* File Input */}
        <input
          type="file"
          ref={fileInputRef}
          style={{ display: "block" }}
          accept=".zip,.tar,.tar.gz,.tgz"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          className="text-input"
        />
        <p style={{ fontSize: "0.8rem", color: "gray" }}>
          Accepted formats: .zip, .tar, .tar.gz, .tgz
        </p>

        {/* Form Fields */}
        <select
          className="dropdown"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          <option value="" disabled hidden>
            Select Category
          </option>
          <option value="Fitness Wearables">Fitness Wearables</option>
          <option value="Smart Home">Smart Home</option>
        </select>

        <input
          type="text"
          placeholder="Enter Device Name"
          value={deviceName}
          onChange={(e) => setDeviceName(e.target.value)}
          className="text-input"
          required
        />

        <input
          type="text"
          placeholder="Manufacturer"
          className="text-input"
          value={manufacturer}
          onChange={(e) => setManufacturer(e.target.value)}
        />

        <input
          type="text"
          placeholder="Operating System"
          className="text-input"
          value={operatingSystem}
          onChange={(e) => setOperatingSystem(e.target.value)}
        />
        <input
          type="text"
          placeholder="OS Version"
          className="text-input"
          value={osVersion}
          onChange={(e) => setOsVersion(e.target.value)}
        />
        <input
          type="text"
          placeholder="Kernel Version"
          className="text-input"
          value={kernelVersion}
          onChange={(e) => setKernelVersion(e.target.value)}
        />

        <button
          className="upload-button"
          onClick={handleUpload}
          disabled={loading || !file || !category || !deviceName}
        >
          {loading ? "Uploading..." : "Upload"}
        </button>

        {showOverlay && (
          <div className="upload-overlay">
            <div className="upload-overlay-content">
              <h3>Uploading...</h3>
              <p>
                Please do not close or switch tabs. This may take a few minutes.
              </p>
            </div>
          </div>
        )}

        {error && <p className="error-message">{error}</p>}
      </div>
    </div>
  );
}
