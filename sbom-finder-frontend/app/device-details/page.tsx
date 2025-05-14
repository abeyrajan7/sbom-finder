"use client";

import { Suspense } from "react";
import DeviceDetailsContent from "./DeviceDetailsContent";

// DeviceDetailsPage wraps content in a Suspense boundary to allow lazy loading
export default function DeviceDetailsPage() {
  return (
    <Suspense fallback={<div>Loading device page...</div>}>
      <DeviceDetailsContent />
    </Suspense>
  );
}
