import sys

def check_braces(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    depth = 0
    for i, line in enumerate(lines):
        line_num = i + 1
        opens = line.count('{')
        closes = line.count('}')
        
        for char in line:
            if char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
                if depth < 0:
                    print(f"Line {line_num}: Extra closing brace! (Depth: {depth})")
                    # depth = 0 # reset to continue?
        
        # print(f"Line {line_num}: Depth {depth}")
    
    if depth > 0:
        print(f"End of file: Missing {depth} closing braces!")
    elif depth < 0:
        print(f"End of file: Extra {abs(depth)} closing braces!")
    else:
        print("Braces are balanced.")

if __name__ == "__main__":
    check_braces(sys.argv[1])
