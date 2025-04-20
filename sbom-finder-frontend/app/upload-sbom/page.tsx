"use client";

import React, { useState, useRef } from "react";
import "./upload.css";

export default function UploadSBOMPage() {
  const BASE_URL = "https://sbom-finder-backend.onrender.com";
  const [uploadType, setUploadType] = useState<"file" | "repo">("file");
  const [file, setFile] = useState<File | null>(null);
  const [repoUrl, setRepoUrl] = useState("");
  const [manufacturer, setManufacturer] = useState("");
  const [category, setCategory] = useState("");
  const [operatingSystem, setOperatingSystem] = useState("");
  const [osVersion, setOsVersion] = useState("");
  const [kernelVersion, setKernelVersion] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [deviceName, setDeviceName] = useState("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleUpload = async () => {
    setError(null);

    if (uploadType === "file" && !file) {
      setError("Please select a SBOM file to upload.");
      return;
    }
    if (uploadType === "repo" && !repoUrl) {
      setError("Please enter a GitHub repo URL.");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    if (uploadType === "file") {
      formData.append("sbomFile", file!);
      formData.append("category", category);
      formData.append("deviceName", deviceName);
      formData.append("manufacturer", manufacturer || "Unknown Manufacturer");
      formData.append("operatingSystem", operatingSystem || "Unknown OS");
      formData.append("osVersion", osVersion || "Unknown Version");
      formData.append("kernelVersion", kernelVersion || "Unknown Kernel");
    }

    try {
      let response;
      if (uploadType === "file") {
        response = await fetch(`${BASE_URL}/api/sboms/upload-sbom`, {
          method: "POST",
          body: formData,
        });
      } else {
        const body = {
          repoUrl,
          category,
          deviceName: "",
          manufacturer: manufacturer || "Unknown Manufacturer",
          operatingSystem: operatingSystem || "Unknown OS",
          osVersion: osVersion || "Unknown Version",
          kernelVersion: kernelVersion || "Unknown Kernel",
        };

        response = await fetch(`${BASE_URL}/api/sboms/from-repo`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
      }

      if (response.ok) {
        const result = await response.text();
        alert(`Success: ${result}`);
        resetForm();
      } else {
        const result = await response.text();
        setError(result);
      }
    } catch (err) {
      console.error("Upload Error:", err);
      setError("Upload failed.");
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFile(null);
    setRepoUrl("");
    setManufacturer("");
    setOperatingSystem("");
    setOsVersion("");
    setKernelVersion("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h2 className="upload-title">Upload SBOM</h2>

        {/* Upload Type Switch */}
        <div className="upload-type-toggle">
          <button
            className={uploadType === "file" ? "active" : ""}
            onClick={() => setUploadType("file")}
          >
            Upload SBOM File
          </button>
          <button
            className={uploadType === "repo" ? "active" : ""}
            onClick={() => setUploadType("repo")}
          >
            Upload from GitHub Repo
          </button>
        </div>

        {/* Upload Form */}
        {uploadType === "file" ? (
          <>
            <input
              type="file"
              ref={fileInputRef}
              style={{ display: uploadType === "file" ? "block" : "none" }}
              accept=".json"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="text-input"
              key={uploadType} // ✅ very important
            />
          </>
        ) : (
          <>
            <input
              type="text"
              placeholder="GitHub Repo URL"
              className="text-input"
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
            />
          </>
        )}

        {/* Common Fields */}
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
          className="text-input" // (use your CSS class or add)
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
          disabled={loading}
        >
          {loading ? "Uploading..." : "Upload"}
        </button>

        {error && <p className="error-message">{error}</p>}
      </div>
    </div>
  );
}
