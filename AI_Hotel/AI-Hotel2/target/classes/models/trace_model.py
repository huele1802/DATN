import torch
import torch.nn as nn
from transformers import AutoModel, AutoTokenizer
import os

class SentenceEmbeddingModel(nn.Module):
    def __init__(self, model_name):
        super(SentenceEmbeddingModel, self).__init__()
        self.model = AutoModel.from_pretrained(model_name)

    def forward(self, input_ids, attention_mask):
        outputs = self.model(input_ids, attention_mask=attention_mask)
        last_hidden_state = outputs.last_hidden_state
        mask = attention_mask.unsqueeze(-1).expand(last_hidden_state.size()).float()
        sum_embeddings = torch.sum(last_hidden_state * mask, dim=1)
        sum_mask = torch.clamp(mask.sum(dim=1), min=1e-9)
        mean_pooled = sum_embeddings / sum_mask
        return mean_pooled

# Tải mô hình và tokenizer
model_name = "sentence-transformers/paraphrase-multilingual-mpnet-base-v2"
tokenizer = AutoTokenizer.from_pretrained(model_name, clean_up_tokenization_spaces=True)
model = SentenceEmbeddingModel(model_name)

# Đầu vào mẫu
input_ids = torch.zeros(1, 512, dtype=torch.long)
attention_mask = torch.ones(1, 512, dtype=torch.long)

# Kiểm tra đầu ra
model.eval()
with torch.no_grad():
    output = model(input_ids, attention_mask)
    print(f"Output shape: {output.shape}")  # Phải là [1, 768]

# Tracing mô hình
traced_model = torch.jit.trace(model, (input_ids, attention_mask))

# Tạo thư mục nếu chưa tồn tại
output_dir = "D:/AI/AI-Hotel2/AI-Hotel2/src/main/resources/models"
os.makedirs(output_dir, exist_ok=True)

# Lưu file
traced_model.save(os.path.join(output_dir, "model.pt"))
print("Model saved successfully")