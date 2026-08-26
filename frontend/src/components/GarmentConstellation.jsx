import { useRef, useMemo, useEffect } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

const COLORS = ['#5C2A4D', '#3D1B33', '#B08968', '#F4E1D2', '#0A0A0F', '#FAFAF8'];

export default function GarmentConstellation({ scale = 1, isAmbient = false, particleCount = 2500 }) {
  const meshRef = useRef();

  const { positions, colors, rotations, scales } = useMemo(() => {
    const pos = new Float32Array(particleCount * 3);
    const col = new Float32Array(particleCount * 3);
    const rot = new Float32Array(particleCount * 3);
    const sca = new Float32Array(particleCount);

    const colorObj = new THREE.Color();

    for (let i = 0; i < particleCount; i++) {
      let x, y, z;
      if (isAmbient) {
        x = (Math.random() - 0.5) * 15;
        y = (Math.random() - 0.5) * 15;
        z = (Math.random() - 0.5) * 10 - 2;
      } else {
        const t = Math.random(); 
        y = (t * 2 - 1) * 3; // -3 to 3
        
        let radius = 0.5;
        if (y < 0) {
          // Skirt flare
          radius = 0.5 + Math.pow(Math.abs(y) / 3, 1.5) * 2;
        } else if (y > 1.5) {
          // Shoulders
          radius = 0.5 + Math.pow((y - 1.5) / 1.5, 2) * 1.2;
        } else {
          // Torso
          radius = 0.5;
        }

        const angle = Math.random() * Math.PI * 2;
        const scatter = (Math.random() - 0.5) * 0.6;
        
        x = Math.cos(angle) * (radius + scatter);
        z = Math.sin(angle) * (radius + scatter) * 0.6; 
        
        y += 0.5;
      }

      pos[i * 3] = x;
      pos[i * 3 + 1] = y;
      pos[i * 3 + 2] = z;

      const hex = COLORS[Math.floor(Math.random() * COLORS.length)];
      colorObj.set(hex);
      col[i * 3] = colorObj.r;
      col[i * 3 + 1] = colorObj.g;
      col[i * 3 + 2] = colorObj.b;

      rot[i * 3] = Math.random() * Math.PI;
      rot[i * 3 + 1] = Math.random() * Math.PI;
      rot[i * 3 + 2] = Math.random() * Math.PI;

      sca[i] = Math.random() * 0.5 + 0.2;
    }

    return { positions: pos, colors: col, rotations: rot, scales: sca };
  }, [isAmbient, particleCount]);

  useEffect(() => {
    if (!meshRef.current) return;
    const dummy = new THREE.Object3D();
    const colorObj = new THREE.Color();
    
    for (let i = 0; i < particleCount; i++) {
      dummy.position.set(positions[i*3], positions[i*3+1], positions[i*3+2]);
      dummy.rotation.set(rotations[i*3], rotations[i*3+1], rotations[i*3+2]);
      dummy.scale.setScalar(scales[i]);
      dummy.updateMatrix();
      meshRef.current.setMatrixAt(i, dummy.matrix);
      
      colorObj.setRGB(colors[i*3], colors[i*3+1], colors[i*3+2]);
      meshRef.current.setColorAt(i, colorObj);
    }
    
    meshRef.current.instanceMatrix.needsUpdate = true;
    if (meshRef.current.instanceColor) {
      meshRef.current.instanceColor.needsUpdate = true;
    }
  }, [positions, colors, rotations, scales, particleCount]);

  useFrame((state) => {
    if (meshRef.current) {
      const time = state.clock.getElapsedTime();
      meshRef.current.rotation.y = time * 0.05;
      meshRef.current.position.y = Math.sin(time * 0.5) * 0.15;
    }
  });

  return (
    <instancedMesh ref={meshRef} args={[null, null, particleCount]} scale={scale}>
      <coneGeometry args={[0.08, 0.15, 3]} />
      <meshBasicMaterial 
        wireframe={true} 
        transparent={true} 
        opacity={isAmbient ? 0.2 : 0.8} 
      />
    </instancedMesh>
  );
}
