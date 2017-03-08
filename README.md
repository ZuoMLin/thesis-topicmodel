## 论文相关主题模型实现
#### 主题模型
LDA模型(LatentDirichletAllocation)  
BTM模型(BitermTopicModel，参考：宴小辉)  
BTOT模型(BitermTopicsOverTime)
#### 在线主题模型
OLDA模型(OnlineLDA)  
OBTM模型(OnlineBTM)  
OBTOT模型(OnlineBTOT)
#### LDA/BTM模型Gibbs采样算法优化
PeacockLDA(腾讯分布式LDA系统，注：只以多线程方式模拟实现)  
APSparseBTM(并行化近似的BTM模型Gibbs采样算法，注：考虑稀疏性)
#### 突发性话题发现
Kleinberg枚举突发框架模拟实现  
BBTM模型(BurstyBitermTopicModel，参考：宴小辉)
