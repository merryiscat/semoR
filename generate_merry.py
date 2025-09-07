#!/usr/bin/env python3
"""
Merry the Siamese Cat - Pixel Art Generator
For SemoR Alarm App

This script provides the framework for generating Merry's pixel art sprites.
Currently set up for manual pixel art creation or integration with AI image generation APIs.
"""

import os
from PIL import Image, ImageDraw
import json

class MerryPixelArtGenerator:
    def __init__(self):
        self.canvas_size = (64, 64)
        self.character_area = (32, 32)  # Core character fits in center 32x32
        self.output_dir = "assets/characters/our_siamese_cat"
        
        # SemoR Brand Colors + Siamese Cat Palette
        self.color_palette = {
            'body_base': '#F5E6D3',      # Cream beige body
            'points_dark': '#3C2415',    # Dark brown points (face, ears, paws)
            'eyes_blue': '#87CEEB',      # Ice blue eyes
            'eyes_glow': '#00D4FF',      # Neon blue - SemoR brand color
            'nose_pink': '#FF69B4',      # Pink nose
            'mouth_brown': '#8B4513',    # Brown mouth interior
            'outline_dark': '#2F1B14',   # Very dark brown outline
            'shadow_cream': '#E5D6C3'    # Darker cream for shadows
        }
        
        # Animation sequences as per CHARACTER_IMAGE_GUIDE.md
        self.animations = {
            'appearing': ['appear_01', 'appear_02', 'appear_03', 'appear_04', 'appear_05'],
            'idle': ['idle_01', 'idle_02', 'idle_03', 'idle_04'],
            'spinning': ['spin_01', 'spin_02', 'spin_03', 'spin_04', 
                        'spin_05', 'spin_06', 'spin_07', 'spin_08'],
            'attention': ['attention_01', 'attention_02', 'attention_03'],
            'urgent': ['urgent_01', 'urgent_02', 'urgent_03', 'urgent_04'],
            'special': ['grooming_01', 'grooming_02', 'grooming_03',
                       'stretch_01', 'stretch_02', 'play_01', 'play_02']
        }
    
    def create_empty_canvas(self):
        """Create a 64x64 transparent canvas"""
        return Image.new('RGBA', self.canvas_size, (0, 0, 0, 0))
    
    def draw_basic_siamese_idle(self):
        """
        Draw the basic idle_01 pose as a foundation
        This is a simplified pixel art representation
        """
        img = self.create_empty_canvas()
        draw = ImageDraw.Draw(img)
        
        # Convert hex colors to RGB
        def hex_to_rgb(hex_color):
            return tuple(int(hex_color[i:i+2], 16) for i in (1, 3, 5))
        
        body_color = hex_to_rgb(self.color_palette['body_base'])
        points_color = hex_to_rgb(self.color_palette['points_dark'])
        eye_color = hex_to_rgb(self.color_palette['eyes_blue'])
        glow_color = hex_to_rgb(self.color_palette['eyes_glow'])
        
        # Basic body shape (simplified for demonstration)
        # In real pixel art, this would be drawn pixel by pixel
        
        # Body oval (center area)
        body_left = 22
        body_top = 28
        body_right = 42
        body_bottom = 50
        draw.ellipse([body_left, body_top, body_right, body_bottom], 
                    fill=body_color + (255,))
        
        # Head circle
        head_left = 26
        head_top = 20
        head_right = 38
        head_bottom = 32
        draw.ellipse([head_left, head_top, head_right, head_bottom], 
                    fill=body_color + (255,))
        
        # Siamese face mask (approximation)
        mask_left = 28
        mask_top = 22
        mask_right = 36
        mask_bottom = 28
        draw.ellipse([mask_left, mask_top, mask_right, mask_bottom], 
                    fill=points_color + (255,))
        
        # Eyes with neon blue glow
        # Left eye
        draw.ellipse([29, 24, 31, 26], fill=eye_color + (255,))
        draw.rectangle([28, 23, 32, 27], outline=glow_color + (128,))  # Glow effect
        
        # Right eye  
        draw.ellipse([33, 24, 35, 26], fill=eye_color + (255,))
        draw.rectangle([32, 23, 36, 27], outline=glow_color + (128,))  # Glow effect
        
        # Ears (triangular)
        draw.polygon([(28, 20), (30, 16), (32, 20)], fill=points_color + (255,))
        draw.polygon([(32, 20), (34, 16), (36, 20)], fill=points_color + (255,))
        
        return img
    
    def save_sprite(self, image, animation_type, frame_name):
        """Save a sprite to the correct directory"""
        directory = os.path.join(self.output_dir, animation_type)
        os.makedirs(directory, exist_ok=True)
        
        filepath = os.path.join(directory, f"{frame_name}.png")
        image.save(filepath, 'PNG')
        return filepath
    
    def generate_idle_01_demo(self):
        """Generate the basic idle_01 frame as a demonstration"""
        print("Generating Merry's idle_01 demo frame...")
        
        img = self.draw_basic_siamese_idle()
        filepath = self.save_sprite(img, 'idle', 'idle_01_demo')
        
        print(f"Demo frame saved: {filepath}")
        print(f"Size: {img.size}")
        print("This is a simplified demo - real pixel art would be hand-crafted pixel by pixel")
        
        return filepath
    
    def create_animation_metadata(self):
        """Create metadata for all planned animations"""
        metadata = {
            'character': 'Merry the Siamese Cat',
            'brand': 'SemoR Alarm App',
            'canvas_size': self.canvas_size,
            'character_area': self.character_area,
            'color_palette': self.color_palette,
            'animations': self.animations,
            'notes': {
                'key_feature': 'Ice blue eyes with neon blue (#00D4FF) SemoR brand highlights',
                'style': '8-bit pixel art, no anti-aliasing',
                'priority': 'idle_01 is the foundation frame for all other animations'
            }
        }
        
        metadata_path = os.path.join(self.output_dir, 'merry_metadata.json')
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        return metadata_path

def main():
    print("Merry the Siamese Cat - Pixel Art Generator")
    print("=" * 50)
    
    generator = MerryPixelArtGenerator()
    
    # Create demo frame
    demo_path = generator.generate_idle_01_demo()
    
    # Create metadata
    metadata_path = generator.create_animation_metadata()
    print(f"Metadata saved: {metadata_path}")
    
    print("\nNext Steps:")
    print("1. Use the detailed specification in merry_pixel_art_spec.md")
    print("2. Generate idle_01.png with proper AI image generation tools")
    print("3. Use idle_01.png as reference for remaining 25 animation frames")
    print("4. Replace demo files with final pixel art")
    
    print(f"\nSemoR Brand Integration:")
    print(f"   Primary Eye Color: {generator.color_palette['eyes_blue']} (Ice Blue)")
    print(f"   Brand Highlight: {generator.color_palette['eyes_glow']} (Neon Blue)")

if __name__ == "__main__":
    main()