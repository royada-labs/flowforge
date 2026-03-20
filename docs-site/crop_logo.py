from PIL import Image, ImageChops

def trim(im):
    bg = Image.new(im.mode, im.size, (255, 255, 255))
    diff = ImageChops.difference(im, bg)
    diff = ImageChops.add(diff, diff, 2.0, -100)
    bbox = diff.getbbox()
    if bbox:
        # Add 1 pixel of margin as requested
        left, top, right, bottom = bbox
        left = max(0, left - 1)
        top = max(0, top - 1)
        right = min(im.size[0], right + 1)
        bottom = min(im.size[1], bottom + 1)
        return im.crop((left, top, right, bottom))
    return im

source_path = '/home/rolando/.gemini/antigravity/brain/10038ab1-f4df-4f54-9b3d-db671ce6307a/flowforge_icon_cliptoedges_white_1774040515727.png'
im = Image.open(source_path)
im = im.convert('RGB')
trimmed = trim(im)
trimmed.save('static/img/logo.png')
trimmed.save('static/img/favicon.ico')
print(f"Original size: {im.size}, Trimmed size: {trimmed.size}")
