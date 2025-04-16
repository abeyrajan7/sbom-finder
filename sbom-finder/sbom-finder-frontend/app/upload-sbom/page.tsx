"use client";

import React, { useRef, useState } from "react";
import "./upload.css";

export default function UploadSBOMPage() {
  const [file, setFile] = useState<File | null>(null);
  const [category, setCategory] = useState("Fitness Wearables");
  const [error, setError] = useState<string | null>(null);
  const [uploadedFileNames, setUploadedFileNames] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const validateSBOMFile = (file: File): Promise<boolean> => {
    return new Promise((resolve) => {
      const reader = new FileReader();

      reader.onload = () => {
        try {
          const content = JSON.parse(reader.result as string);
          const isSPDX = !!content.spdxVersion;
          const isCycloneDX = content.bomFormat === "CycloneDX";
          const isCPE =
            Array.isArray(content.components) &&
            content.components.some((comp: { cpe?: string }) => !!comp.cpe)
          if (!isSPDX && !isCycloneDX && !isCPE) {
            setError("Upload SPDX, CycloneDX, or CPE formatted SBOM files.");
            resolve(false);
            return;
          }

          resolve(true);
        } catch {
          setError("Invalid JSON structure.");
          resolve(false);
        }
      };

      reader.readAsText(file);
    });
  };

    const fileInputRef = useRef<HTMLInputElement>(null);

  const resetFileInput = () => {
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    };

  const handleUpload = async () => {
    setError(null);

    if (!file) {
      setError("Please select a file.");
      return;
    }

    setLoading(true); // start loading
    setError(null);

    // ✅ Check file extension
    if (!file.name.endsWith(".json")) {
      setError("Upload file expected to be in JSON format.");
      return;
    }

    // ✅ Check for duplicates
    if (uploadedFileNames.includes(file.name)) {
      setError("This SBOM file has already been uploaded.");
      return;
    }

    // ✅ Check file content
    const isValid = await validateSBOMFile(file);
    if (!isValid) return;

    // ✅ Upload
    const formData = new FormData();
    formData.append("sbomFile", file);
    formData.append("category", category);

    try {
      const response = await fetch(
        "http://localhost:8080/api/sboms/upload-sbom",
        {
          method: "POST",
          body: formData,
        }
      );

      if (response.ok) {
          resetFileInput();
          const result = await response.text();
        alert(`Success: ${result}`);
        setUploadedFileNames((prev) => [...prev, file.name]);
      } else if (response.status === 409) {
          setLoading(false)
          resetFileInput();
          const result = await response.text();
          setError(result);
      } else {
        setError("Upload failed.");
      }
    } catch (error) {
        setLoading(false)
        console.error("Upload error:", error);
        setError("Upload error occurred");
    } finally {
        setLoading(false);
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h2 className="upload-title">Upload SBOM File</h2>

        <label htmlFor="category">Select Category:</label>
        <select
          id="category"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="dropdown"
        >
          <option value="Fitness Wearables">Fitness Wearables</option>
          <option value="Smart Home">Smart Home</option>
        </select>

        <input
          type="file"
          ref={fileInputRef}
          className="file-input"
          accept=".json"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />

        <button className="upload-button" onClick={handleUpload}>
          Upload
        </button>

        {loading ? (
          <p style={{ color: "blue", marginTop: "10px" }}>Uploading...</p>
        ) : (
          file && <p className="file-name">Selected: {file.name}</p>
        )}
        {error && <p style={{ color: "red", marginTop: "10px" }}>{error}</p>}
      </div>
    </div>
  );
}
