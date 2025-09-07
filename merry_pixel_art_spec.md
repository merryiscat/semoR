# 🐱 Merry the Siamese Cat - Pixel Art Specification

## 📏 Technical Requirements

### Canvas & Format
- **Size**: 64x64 pixels (exact)
- **Format**: PNG with transparency
- **Background**: Fully transparent (Alpha = 0)
- **Character Area**: Centered within 32x32px core area (16px margin on all sides)
- **Style**: Sharp pixel art, NO anti-aliasing
- **Palette**: Maximum 8 colors

### Character: Merry - Idle_01 Pose

#### Physical Description
- **Species**: Siamese cat
- **Pose**: Sitting, front-facing view
- **Body Position**: Upright sitting, tail curled around front paws
- **Head**: Facing directly forward
- **Eyes**: Looking straight ahead

#### Color Palette (SemoR Brand Integration)
```
1. Base Body: #F5E6D3 (Cream/Beige)
2. Points (face mask, ears, paws): #3C2415 (Dark Brown)
3. Eyes: #87CEEB (Ice Blue) - PRIMARY
4. Eye Highlights: #00D4FF (Neon Blue) - SemoR Brand Color
5. Nose: #FF69B4 (Pink)
6. Inner Mouth: #8B4513 (Brown)
7. Outline/Details: #2F1B14 (Very Dark Brown)
8. Shadow/Depth: #E5D6C3 (Darker Cream)
```

#### Detailed Features

##### Face & Head (Top Third - 20px tall)
- **Face Mask**: Dark brown (#3C2415) covering upper face in classic Siamese pattern
- **Ears**: Triangle shape, dark brown with pink inner ear
- **Eyes**: Large, prominent ice blue (#87CEEB) with neon blue (#00D4FF) rim highlights
  - 3x3 or 4x4 pixels each eye
  - Small white highlight dot in each eye
  - Subtle neon blue glow around eye edges
- **Nose**: Small pink triangle, 2x2 pixels
- **Mouth**: Subtle curve suggested with darker outline

##### Body (Middle Third - 20px tall)
- **Torso**: Cream beige (#F5E6D3) oval/rounded rectangle
- **Front Legs**: Visible, cream colored with dark brown paw tips
- **Chest**: Slightly lighter cream for dimension

##### Lower Body & Tail (Bottom Third - 20px tall)
- **Hind Legs**: Sitting position, cream colored with dark brown paw points
- **Tail**: Curled around front, dark brown tip, cream base
- **Base/Ground**: Minimal shadow suggestion

#### Positioning & Proportions
- **Head**: 12-14px wide, 16-18px tall including ears
- **Body**: 18-20px wide, 20-24px tall total
- **Center Point**: Pixel 32,32 of 64x64 canvas
- **Vertical Alignment**: Character base at pixel row 48-50

#### SemoR Brand Integration
- **Neon Blue Highlights**: Subtle #00D4FF accents around eyes
- **Color Harmony**: Ice blue eyes complement SemoR's neon blue (#00D4FF)
- **Professional Quality**: Clean, recognizable, brand-appropriate

## 🎯 Generation Prompts

### Primary Prompt (Detailed)
"64x64 pixel art of a Siamese cat named Merry, sitting front-view. Cream beige body (#F5E6D3), dark brown Siamese points (#3C2415) on face/ears/paws. Large ice blue eyes (#87CEEB) with subtle neon blue (#00D4FF) glow highlights - this is crucial for brand identity. Pink nose, sitting elegantly with tail curled around paws. Transparent background, sharp pixels only, 8-color palette max, centered in 32x32px core area. 8-bit retro gaming style."

### Alternative Prompt (Simplified)
"Create 64x64 pixel art sprite: Siamese cat sitting front-view, cream body with dark points, most important feature is ice blue eyes with neon blue highlights (#00D4FF), transparent background, sharp pixel art style, centered, no anti-aliasing"

### Style References
- **Inspiration**: 16-bit JRPG pet companions
- **Quality**: Pokemon sprite level detail
- **Mood**: Friendly, approachable, slightly regal (Siamese dignity)

## 📁 File Output
- **Filename**: `merry_idle_01.png`
- **Location**: `C:\Users\minhy\Study\LLM_Study\semoR\assets\characters\our_siamese_cat\idle\`
- **Verification**: Must be exactly 64x64px, PNG format, transparent background

## ✅ Quality Checklist
- [ ] Exactly 64x64 pixels
- [ ] Transparent background (no white background)
- [ ] Sharp pixel art (no blurring/anti-aliasing)
- [ ] Siamese color pattern correct
- [ ] Ice blue eyes with neon blue (#00D4FF) highlights visible
- [ ] Character centered in frame
- [ ] Maximum 8 colors used
- [ ] Front-facing sitting pose
- [ ] Professional, brand-appropriate quality

This specification provides the exact requirements for creating Merry's idle_01 pose that will serve as the foundation for the complete animation sequence outlined in CHARACTER_IMAGE_GUIDE.md.