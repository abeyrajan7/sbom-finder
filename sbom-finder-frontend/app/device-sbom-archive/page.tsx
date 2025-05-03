"use client";

import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './device-sbom-archive.css';

interface ArchiveEntry {
  archiveId: number;
  name: string;
  isLatest: boolean;
}

interface DeviceArchiveData {
  deviceName: string;
  archives: ArchiveEntry[];
}

const SbomArchiveDownload: React.FC = () => {
  const [deviceData, setDeviceData] = useState<DeviceArchiveData[]>([]);
  const [selectedArchiveId, setSelectedArchiveId] = useState<number | null>(null);
  const [showDialog, setShowDialog] = useState(false);
  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

  useEffect(() => {
    axios.get(`${BASE_URL}/api/devices/archives/all`)
      .then(res => setDeviceData(res.data))
      .catch(err => console.error('Error fetching device archive data', err));
  }, [BASE_URL]);

  const handleDownload = (archiveId: number, format: string) => {
    const url = `${BASE_URL}/api/devices/download/archive/${archiveId}?format=${format}`;
    window.open(url, '_blank');
    setShowDialog(false);
  };

  return (
    <div className="sbom-archive-container">
      <h2>SBOM Archives</h2>
      <div className="archive-table">
        {deviceData.map(device => (
          <div key={device.deviceName} className="archive-row">
            <div className="device-name">{device.deviceName}</div>
            <div className="device-versions">
              {device.archives
                .slice()
                .sort((a, b) => (a.isLatest === b.isLatest ? 0 : a.isLatest ? -1 : 1))
                .map((archive) => (
                  <div
                    key={archive.archiveId}
                    className="version-link"
                    onClick={() => {
                      setSelectedArchiveId(archive.archiveId);
                      setShowDialog(true);
                    }}
                  >
                    {archive.name} {archive.isLatest ? '(latest)' : ''}
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>

      {showDialog && selectedArchiveId && (
        <div className="download-dialog">
          <div className="dialog-box">
            <p>Select format to download:</p>
            <button onClick={() => handleDownload(selectedArchiveId, 'cyclonedx')}>CycloneDX</button>
            <button onClick={() => handleDownload(selectedArchiveId, 'spdx')}>SPDX</button>
            <button className="cancel-button" onClick={() => setShowDialog(false)}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default SbomArchiveDownload;
